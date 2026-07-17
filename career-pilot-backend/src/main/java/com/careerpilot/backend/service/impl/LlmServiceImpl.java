package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.ENUMs.DocType;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.IRagContextDocumentRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements ILlmService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final IQuestionBankRepository questionBankRepository;
    private final IFeedbackReportRepository feedbackReportRepository;
    private final IRagContextDocumentRepository ragContextDocumentRepository;

    @Override
    public List<GeneratedQuestion> generateQuestions(Long trackId, int count) {
        List<QuestionBank> baseQuestions = questionBankRepository.findByTrackId(trackId);
        String samples = baseQuestions.stream()
                .map(q -> "- ID " + q.getId() + ": " + q.getQuestionText()
                        + "  [keywords: " + q.getExpectedKeywords() + "]")
                .collect(Collectors.joining("\n"));

        String prompt = """
                Here are the source questions for this track:
                
                %s
                
                Generate %d interview questions based on these samples.
                For each source question, rephrase it or create a real-world scenario that tests the same concept.
                Distribute the %d questions evenly across the available source questions.
                
                Return ONLY a JSON array with no other text. Each object must have:
                  - "text": the generated question string
                  - "sourceQuestionId": the numeric ID of the source question it is based on (from the list above)
                
                Example:
                [{"text": "Explain how arrays differ from linked lists in memory usage", "sourceQuestionId": 1}]
                """.formatted(samples, count, count);

        String response = chatClient.prompt()
                .system(s -> s.text("""
                        You are a technical interview question generator.
                        Your job is to create varied, realistic interview questions that test the same concepts
                        as the provided sample questions but use different wording, real-world scenarios, or angles.
                        Each generated question must link back to its source question via sourceQuestionId.
                        """))
                .user(prompt)
                .call()
                .content();

        try {
            return objectMapper.readValue(response, new TypeReference<List<GeneratedQuestion>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse LLM response for generateQuestions: {}", response, e);
            return List.of();
        }
    }

    @Override
    public ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript) {
        String idealAnswer = buildIdealAnswerContext(questionId);
        String history = buildUserHistoryContext(userId);
        String cv = buildCvContext(userId);
        String sessionCtx = buildSessionContext(userId);

        String prompt = """
                Score this interview answer.
                
                Question being answered: %s
                
                Ideal answer reference:
                %s
                
                Candidate's past performance:
                %s
                
                Candidate's CV context:
                %s
                
                Session context:
                %s
                
                Candidate answer: %s
                
                Score 0-100 on:
                - Content Relevance: does the answer cover the expected keywords and concepts?
                - Clarity: well-structured, coherent, grammatical?
                - Confidence: hedging language? ("I think", "maybe", self-correction)
                - Filler words: count of "um/uh/like/you know"
                
                Use the ideal answer reference to determine if the candidate hit the key points.
                Use the past performance and CV to calibrate expectations.
                
                Return ONLY raw JSON with no markdown formatting.
                {"contentRelevance": 0, "clarity": 0, "confidence": 0, "fillerWords": 0, "reasoning": "..."}
                """.formatted(questionId, idealAnswer, history, cv, sessionCtx, transcript);

        String response = chatClient.prompt()
                .system(s -> s.text("""
                        You are an expert interview evaluator with 15+ years of experience.
                        Score answers against the ideal answer keywords. Be critical — a score of 100 means the answer was perfect, which is rare.
                        """))
                .user(prompt)
                .call()
                .content();

        try {
            return objectMapper.readValue(stripMarkdown(response), ScoreResponse.class);
        } catch (Exception e) {
            log.warn("Failed to parse score response: {}", response, e);
            return new ScoreResponse(0, 0, 0, 0, "Could not evaluate answer");
        }
    }

    @Override
    public List<String> generateTips(String transcript, ScoreResponse scores) {
        String prompt = """
                Based on this interview answer and scores, generate 3-5 coaching tips.
                
                Scores:
                - Content Relevance: %s/100
                - Clarity: %s/100
                - Confidence: %s/100
                - Filler words: %s/100
                
                Reason: %s
                
                Transcript: %s
                
                Return ONLY a JSON array of strings, each tip is ranked by importance.
                Example: ["Focus on structuring your answer with STAR method", "Work on reducing filler words"]
                """.formatted(
                        scores.contentRelevance(), scores.clarity(),
                        scores.confidence(), scores.fillerWords(),
                        scores.reasoning(), transcript);

        String response = chatClient.prompt()
                .system(s -> s.text("""
                        You are a senior career coach helping candidates improve their interview performance.
                        Provide specific, actionable, and constructive feedback.
                        Each tip should be a concrete recommendation, not generic advice.
                        """))
                .user(prompt)
                .call()
                .content();

        try {
            return objectMapper.readValue(response, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(response);
        }
    }

    @Override
    public CvAnalysis analyzeCv(String cvText) {
        log.info("LLM analyzeCv - input text length: {} chars", cvText.length());
        log.debug("LLM analyzeCv - input preview: {}...", cvText.substring(0, Math.min(cvText.length(), 300)));

        String prompt = """
                Extract structured information from this CV text.
                
                CV: %s
                
                Return ONLY raw JSON with no markdown formatting.
                {"skills": [], "yearsOfExperience": 0, "targetRole": "", "educationLevel": ""}
                """.formatted(cvText);

        log.info("LLM analyzeCv - sending request...");
        String response = chatClient.prompt()
                .system(s -> s.text("""
                        You are an HR expert specializing in CV parsing and skills assessment.
                        Extract structured information from unstructured CV text accurately.
                        If a field cannot be determined, use reasonable defaults or empty lists.
                        """))
                .user(prompt)
                .call()
                .content();

        log.info("LLM analyzeCv - raw response length: {} chars", response != null ? response.length() : 0);
        log.info("LLM analyzeCv - raw response: {}", response);

        String stripped = stripMarkdown(response);
        log.info("LLM analyzeCv - stripped: {}", stripped);

        try {
            CvAnalysis result = objectMapper.readValue(stripped, CvAnalysis.class);
            log.info("LLM analyzeCv - parsed OK: skills={}, targetRole={}, years={}, education={}",
                result.skills(), result.targetRole(), result.yearsOfExperience(), result.educationLevel());
            return result;
        } catch (Exception e) {
            log.warn("LLM analyzeCv - FAILED to parse response: {}", response, e);
            return new CvAnalysis(List.of(), 0, "", "");
        }
    }

    private String buildIdealAnswerContext(Long questionId) {
        Optional<QuestionBank> opt = questionBankRepository.findById(questionId);
        if (opt.isEmpty()) return "No reference answer available.";
        QuestionBank q = opt.get();
        StringBuilder sb = new StringBuilder();
        sb.append("Question: ").append(q.getQuestionText()).append("\n");
        if (q.getExpectedKeywords() != null && !q.getExpectedKeywords().isBlank())
            sb.append("Expected keywords: ").append(q.getExpectedKeywords()).append("\n");
        sb.append("Difficulty: ").append(q.getDifficultyLevel()).append("\n");
        sb.append("Category: ").append(q.getCategory());
        return sb.toString();
    }

    private String buildUserHistoryContext(Long userId) {
        List<FeedbackReport> reports = feedbackReportRepository
                .findBySessionUserIdOrderByCreatedAtDesc(userId);
        if (reports.isEmpty()) return "No past session history available.";

        double avgScore = reports.stream()
                .mapToInt(FeedbackReport::getOverallScore)
                .average()
                .orElse(0);

        List<String> tips = reports.stream()
                .filter(r -> r.getCoachingTips() != null)
                .map(FeedbackReport::getCoachingTips)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("Past sessions: ").append(reports.size()).append("\n");
        sb.append("Average score: ").append(String.format("%.1f", avgScore)).append("\n");
        if (!tips.isEmpty()) {
            sb.append("Recurring advice:\n");
            tips.stream().limit(3).forEach(t -> sb.append("- ").append(t).append("\n"));
        }
        return sb.toString();
    }

    private String buildCvContext(Long userId) {
        List<RagContextDocument> docs = ragContextDocumentRepository
                .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.CV_EXTRACT);
        if (docs.isEmpty()) return "No CV context available.";
        return docs.get(0).getContent();
    }

    private String buildSessionContext(Long userId) {
        List<RagContextDocument> docs = ragContextDocumentRepository
                .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.SESSION_SUMMARY);
        if (docs.isEmpty()) return "No session context available.";
        return docs.stream()
                .limit(3)
                .map(RagContextDocument::getContent)
                .collect(Collectors.joining("\n---\n"));
    }

    private String stripMarkdown(String raw) {
        if (raw == null) return "{}";
        return raw.replaceAll("```(?:json)?\\s*", "").trim();
    }
}
