package com.careerpilot.backend.service.agent;

import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewTools {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final IQuestionBankRepository questionBankRepository;
    private final WebSearchService webSearchService;

    @Tool(description = "Evaluate a candidate's interview answer and return scores as a JSON string. " +
            "Returns: {\"contentRelevance\": 0-100, \"clarity\": 0-100, \"confidence\": 0-100, \"fillerWords\": 0-100, \"reasoning\": \"...\"}")
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
                .system("You are an expert interview evaluator. Be critical and specific.")
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
    public String searchQuestionBank(String trackName, String topic) {
        List<QuestionBank> allQuestions = questionBankRepository.findAll();
        String lowerTopic = topic != null ? topic.toLowerCase() : "";
        String lowerTrack = trackName != null ? trackName.toLowerCase() : "";

        List<QuestionBank> matches = allQuestions.stream()
                .filter(q -> q.getTrack() != null)
                .filter(q -> {
                    String name = q.getTrack().getName() != null ? q.getTrack().getName().toLowerCase() : "";
                    String cat = q.getCategory() != null ? q.getCategory().name().toLowerCase() : "";
                    String text = q.getQuestionText() != null ? q.getQuestionText().toLowerCase() : "";
                    String kw = q.getExpectedKeywords() != null ? q.getExpectedKeywords().toLowerCase() : "";
                    return name.contains(lowerTrack) || cat.contains(lowerTopic)
                            || text.contains(lowerTopic) || kw.contains(lowerTopic);
                })
                .limit(8)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return "No matching questions found in the question bank for topic: " + topic;
        }

        StringBuilder sb = new StringBuilder("Question bank matches:\n");
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

    @Tool(description = "Search the web for current interview questions, industry trends, or best practices on any topic. " +
            "Use this to find fresh, up-to-date questions that reflect current industry standards.")
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
}
