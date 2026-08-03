package com.careerpilot.backend.service.agent;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.repository.IJobListingRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewTools {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final IQuestionBankRepository questionBankRepository;
    private final IJobListingRepository jobListingRepository;
    private final WebSearchService webSearchService;

    @Tool(description = "Evaluate a candidate's interview answer and return scores as a JSON string. " +
            "Returns: {\"contentRelevance\": 0-100, \"clarity\": 0-100, \"confidence\": 0-100, \"fillerWords\": 0-100, \"reasoning\": \"...\"}")
    @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
    public String evaluateAnswer(String questionText, String transcript, String idealAnswerKeywords) {
        String prompt = """
                Score this interview answer.
                
                Question: %s
                
                Ideal answer keywords: %s
                
                Candidate answer: %s
                
                Score 0-100 on:
                - contentRelevance: does the answer cover the expected keywords?
                - clarity: well-structured and coherent?
                - confidence: minimal hedging language?
                - fillerWords: fewer filler words = higher score (100 = none, 0 = many)
                
                Return ONLY raw JSON: {"contentRelevance": 0, "clarity": 0, "confidence": 0, "fillerWords": 0, "reasoning": "brief explanation"}
                """.formatted(questionText,
                idealAnswerKeywords != null ? idealAnswerKeywords : "General best practices",
                transcript);

        String response = chatClient.prompt()
                .system("You are an expert interview evaluator. Be critical and specific. "
                        + "Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.")
                .user(prompt)
                .call()
                .content();

        try {
            ScoreResponse parsed = objectMapper.readValue(
                    response.replaceAll("```(?:json)?\\s*", "").trim(),
                    ScoreResponse.class);
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            log.warn("Failed to parse evaluateAnswer response: {}", response, e);
            return """
                    {"contentRelevance": 0, "clarity": 0, "confidence": 0, "fillerWords": 0, "reasoning": "Evaluation failed"}
                    """;
        }
    }

    @Tool(description = "Search the question bank for sample questions matching a topic or skill. " +
            "Returns a formatted list of questions with their difficulty and keywords.")
    @RateLimit(capacity = 30, refillTokens = 30, refillSeconds = 60)
    public String searchQuestionBank(String trackName, String topic) {
        List<QuestionBank> allQuestions = questionBankRepository.findByIsActiveTrue();
        List<String> topicTokens = tokenize(topic);
        List<String> trackTokens = tokenize(trackName);
        String lowerTopic = topic != null ? topic.toLowerCase() : "";
        String lowerTrack = trackName != null ? trackName.toLowerCase() : "";

        Map<Long, Double> scores = new HashMap<>();
        for (QuestionBank q : allQuestions) {
            if (q.getTrack() == null) {
                continue;
            }
            scores.put(q.getId(), scoreQuestion(q, topicTokens, trackTokens, lowerTopic, lowerTrack));
        }

        List<QuestionBank> matches = allQuestions.stream()
                .filter(q -> q.getTrack() != null)
                .filter(q -> scores.getOrDefault(q.getId(), 0.0) > 0)
                .sorted(Comparator.comparingDouble((QuestionBank q) -> scores.getOrDefault(q.getId(), 0.0)).reversed())
                .limit(8)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return "No matching questions found in the question bank for topic: " + topic;
        }

        StringBuilder sb = new StringBuilder("Question bank matches (best match first):\n");
        for (QuestionBank q : matches) {
            sb.append("- [").append(q.getDifficultyLevel()).append("] ")
                    .append(q.getQuestionText()).append("\n");
            if (q.getExpectedKeywords() != null && !q.getExpectedKeywords().isBlank()) {
                sb.append("  Keywords: ").append(q.getExpectedKeywords()).append("\n");
            }
            sb.append("  Category: ").append(q.getCategory()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * Lexical relevance score: exact topic phrase hits in text/keywords/category
     * score highest, then per-token hits weighted by where they occur
     * (keywords > text > category), plus a small boost when the question's track
     * matches the requested track.
     */
    private double scoreQuestion(QuestionBank q, List<String> topicTokens, List<String> trackTokens,
                                 String lowerTopic, String lowerTrack) {
        String text = q.getQuestionText() != null ? q.getQuestionText().toLowerCase() : "";
        String kw = q.getExpectedKeywords() != null ? q.getExpectedKeywords().toLowerCase() : "";
        String cat = q.getCategory() != null ? q.getCategory().name().toLowerCase() : "";
        String tname = q.getTrack().getName() != null ? q.getTrack().getName().toLowerCase() : "";

        double score = 0;
        if (!lowerTopic.isBlank() && text.contains(lowerTopic)) score += 6;
        if (!lowerTopic.isBlank() && kw.contains(lowerTopic)) score += 5;
        if (!lowerTopic.isBlank() && cat.contains(lowerTopic)) score += 4;

        for (String tok : topicTokens) {
            if (text.contains(tok)) score += 2;
            if (kw.contains(tok)) score += 3;
            if (cat.contains(tok)) score += 2;
        }
        for (String tok : trackTokens) {
            if (!tok.isBlank() && tname.contains(tok)) score += 1.5;
        }
        return score;
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return Arrays.stream(text.toLowerCase().split("[^a-z0-9]+"))
                .filter(t -> !t.isBlank())
                .collect(Collectors.toList());
    }

    @Tool(description = "Search the web for current interview questions, industry trends, or best practices on any topic. " +
            "Use this to find fresh, up-to-date questions that reflect current industry standards.")
    @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
    public String searchWeb(String query) {
        return webSearchService.search(query);
    }

    @Tool(description = "Check if the interview session should end based on time elapsed and questions asked. " +
            "Returns: \"continue\" if the session should continue, or \"complete\" if the session should end.")
    public String checkCompletion(int questionsAsked, int maxQuestions, int elapsedSeconds, int targetDurationSeconds) {
        if (questionsAsked >= maxQuestions) {
            return "complete";
        }
        if (elapsedSeconds >= targetDurationSeconds) {
            return "complete";
        }
        return "continue";
    }

    @Tool(description = "Retrieve the full job posting (description, responsibilities, qualifications) for a saved job " +
            "by its job ID. Use this to probe a specific requirement of the role being interviewed for. " +
            "Returns a formatted job posting or an error if the job id is unknown.")
    @RateLimit(capacity = 30, refillTokens = 30, refillSeconds = 60)
    public String getJobPosting(Long jobId) {
        Optional<JobListing> opt = jobListingRepository.findById(jobId);
        if (opt.isEmpty()) {
            return "No job posting found for id: " + jobId;
        }
        JobListing j = opt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Job Title: ").append(j.getTitle()).append("\n");
        if (j.getCompanyName() != null) sb.append("Company: ").append(j.getCompanyName()).append("\n");
        if (j.getLocation() != null) sb.append("Location: ").append(j.getLocation()).append("\n");
        if (j.getEmploymentType() != null) sb.append("Employment Type: ").append(j.getEmploymentType()).append("\n");
        if (j.getSeniorityLevel() != null) sb.append("Seniority: ").append(j.getSeniorityLevel()).append("\n");
        if (j.getRequiredSkills() != null && !j.getRequiredSkills().isEmpty())
            sb.append("Required Skills: ").append(String.join(", ", j.getRequiredSkills())).append("\n");
        if (j.getPreferredSkills() != null && !j.getPreferredSkills().isEmpty())
            sb.append("Preferred Skills: ").append(String.join(", ", j.getPreferredSkills())).append("\n");
        if (j.getTechnologies() != null && !j.getTechnologies().isEmpty())
            sb.append("Technologies: ").append(String.join(", ", j.getTechnologies())).append("\n");
        if (j.getDescription() != null && !j.getDescription().isBlank())
            sb.append("\nDescription:\n").append(j.getDescription()).append("\n");
        if (j.getResponsibilities() != null && !j.getResponsibilities().isBlank())
            sb.append("\nResponsibilities:\n").append(j.getResponsibilities()).append("\n");
        if (j.getQualifications() != null && !j.getQualifications().isBlank())
            sb.append("\nQualifications:\n").append(j.getQualifications()).append("\n");
        return sb.toString();
    }
}
