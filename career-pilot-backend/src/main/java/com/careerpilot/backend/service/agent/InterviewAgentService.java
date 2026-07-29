package com.careerpilot.backend.service.agent;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.annotation.RedactPii;
import com.careerpilot.backend.entity.ENUMs.DocType;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.IRagContextDocumentRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewAgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final IQuestionBankRepository questionBankRepository;
    private final IFeedbackReportRepository feedbackReportRepository;
    private final IRagContextDocumentRepository ragContextDocumentRepository;
    private final ISessionQuestionRepository sessionQuestionRepository;
    private final InterviewTools interviewTools;

    @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
    @RedactPii
    public AgentResponse processTurn(
            Long userId, Long sessionId,
            String transcript, String currentQuestionText, Long currentQuestionBankId,
            String trackName, String trackDescription,
            List<SessionQuestion> history,
            int questionsAsked, int maxQuestions,
            int elapsedSeconds, int targetDurationSeconds
    ) {
        String cvContext = buildCvContext(userId);
        String pastPerformance = buildUserHistoryContext(userId);
        String questionBankContext = buildQuestionBankContext(trackName);
        String historyText = buildHistoryText(history);
        String idealAnswerKeywords = buildIdealAnswerKeywords(currentQuestionBankId);

        String systemPrompt = """
                You are Career Pilot AI — an expert interview conductor. Your job is to:
                1. Evaluate the candidate's latest answer
                2. Decide what to do next (probe deeper, switch topic, or end)
                3. Generate a coaching tip
                
                You have access to tools. Use them wisely:
                - evaluateAnswer: score the candidate's answer (always call this first)
                - searchQuestionBank: find relevant questions from the question bank
                - searchWeb: find current/trending interview questions on any topic
                - checkCompletion: check if the session should end
                
                ALWAYS call evaluateAnswer first to score the response.
                Then decide: if the answer was shallow or incomplete, generate a probing follow-up.
                If the answer was thorough, move to a new topic.
                If the session is complete, signal that.
                
                After using the tools, produce your final response as raw JSON with this exact format:
                {"scores":{"contentRelevance":0,"clarity":0,"confidence":0,"fillerWords":0,"reasoning":""},"nextQuestion":"","sourceQuestionId":null,"coachingTip":"","sessionStatus":"IN_PROGRESS"}
                
                sessionStatus must be "IN_PROGRESS" to continue or "READY_TO_COMPLETE" to end.
                sourceQuestionId should be the ID of a question bank question if it matches, or null for custom questions.
                """;

        String userPrompt = """
                Track: %s
                Track Objective: %s
                
                Candidate CV:
                %s
                
                Past Performance:
                %s
                
                Question Bank Reference:
                %s
                
                Current question: %s
                Ideal answer keywords: %s
                
                Conversation history:
                %s
                
                Candidate's latest answer: %s
                
                Session progress: %d of %d questions asked, %d of %d seconds elapsed.
                
                Evaluate, decide, and respond.
                """.formatted(trackName,
                trackDescription != null ? trackDescription : "Assess the candidate's skills",
                cvContext, pastPerformance, questionBankContext,
                currentQuestionText,
                idealAnswerKeywords != null ? idealAnswerKeywords : "General best practices",
                historyText,
                transcript != null ? transcript : "[No answer provided]",
                questionsAsked, maxQuestions, elapsedSeconds, targetDurationSeconds);

        log.info("Agent processing turn for session {} (Q#{}/{}", sessionId, questionsAsked, maxQuestions);

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .tools(interviewTools)
                .call()
                .content();

        return parseAgentResponse(response, currentQuestionText, transcript);
    }

    private AgentResponse parseAgentResponse(String response, String questionText, String transcript) {
        if (response == null || response.isBlank()) {
            return AgentResponse.fallback(questionText);
        }

        try {
            String cleaned = response.replaceAll("```(?:json)?\\s*", "").trim();
            JsonNode root = objectMapper.readTree(cleaned);

            JsonNode scoresNode = root.get("scores");
            int contentRelevance = getInt(scoresNode, "contentRelevance");
            int clarity = getInt(scoresNode, "clarity");
            int confidence = getInt(scoresNode, "confidence");
            int fillerWords = getInt(scoresNode, "fillerWords");
            String reasoning = getString(scoresNode, "reasoning");

            String nextQuestion = getString(root, "nextQuestion");
            Long sourceQuestionId = root.has("sourceQuestionId") && !root.get("sourceQuestionId").isNull()
                    ? root.get("sourceQuestionId").asLong()
                    : null;
            String coachingTip = getString(root, "coachingTip");
            String sessionStatus = getString(root, "sessionStatus");
            if (!"READY_TO_COMPLETE".equals(sessionStatus)) {
                sessionStatus = "IN_PROGRESS";
            }

            return AgentResponse.builder()
                    .contentRelevance(contentRelevance)
                    .clarity(clarity)
                    .confidence(confidence)
                    .fillerWords(fillerWords)
                    .reasoning(reasoning)
                    .nextQuestion(nextQuestion)
                    .sourceQuestionId(sourceQuestionId)
                    .coachingTip(coachingTip)
                    .sessionStatus(sessionStatus)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse agent response: {}", response, e);
            return AgentResponse.fallback(questionText);
        }
    }

    private int getInt(JsonNode node, String field) {
        if (node != null && node.has(field)) {
            return node.get(field).asInt(0);
        }
        return 0;
    }

    private String getString(JsonNode node, String field) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText("");
        }
        return "";
    }

    private String buildCvContext(Long userId) {
        List<RagContextDocument> docs = ragContextDocumentRepository
                .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.CV_EXTRACT);
        if (docs.isEmpty()) return "No CV provided.";
        return PiiRedactionUtil.redact(docs.get(0).getContent());
    }

    private String buildUserHistoryContext(Long userId) {
        List<FeedbackReport> reports = feedbackReportRepository
                .findBySessionUserIdOrderByCreatedAtDesc(userId);
        if (reports.isEmpty()) return "No past session history.";
        double avgScore = reports.stream()
                .mapToInt(FeedbackReport::getOverallScore)
                .average().orElse(0);
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

    private String buildQuestionBankContext(String trackName) {
        List<QuestionBank> questions = questionBankRepository.findAll().stream()
                .filter(q -> q.getTrack() != null && trackName != null
                        && q.getTrack().getName().toLowerCase().contains(trackName.toLowerCase()))
                .limit(10)
                .collect(Collectors.toList());
        if (questions.isEmpty()) return "No question bank entries for this track.";
        StringBuilder sb = new StringBuilder("Available sample questions:\n");
        for (QuestionBank q : questions) {
            sb.append("- [").append(q.getDifficultyLevel()).append("] ")
                    .append(q.getQuestionText()).append("\n");
            if (q.getExpectedKeywords() != null) {
                sb.append("  Keywords: ").append(q.getExpectedKeywords()).append("\n");
            }
            sb.append("  Category: ").append(q.getCategory()).append("\n");
        }
        return sb.toString();
    }

    private String buildHistoryText(List<SessionQuestion> history) {
        if (history == null || history.isEmpty()) {
            return "No previous questions. This is the first exchange.";
        }
        StringBuilder sb = new StringBuilder();
        for (SessionQuestion sq : history) {
            sb.append("Q: ").append(sq.getQuestionText()).append("\n");
            sb.append("A: ").append(sq.getUserTranscript() != null ? sq.getUserTranscript() : "[skipped]").append("\n\n");
        }
        return sb.toString();
    }

    private String buildIdealAnswerKeywords(Long questionBankId) {
        if (questionBankId == null) return null;
        return questionBankRepository.findById(questionBankId)
                .map(QuestionBank::getExpectedKeywords)
                .orElse(null);
    }
}
