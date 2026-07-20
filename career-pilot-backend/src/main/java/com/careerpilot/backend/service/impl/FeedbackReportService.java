package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.FeedbackReportResponse;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.service.IFeedbackReportService;
import com.careerpilot.backend.service.ILlmService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackReportService implements IFeedbackReportService {

    private final IFeedbackReportRepository feedbackReportRepository;
    private final IInterviewSessionRepository sessionRepository;
    private final ISessionQuestionRepository sessionQuestionRepository;
    private final IQuestionScoreRepository scoreRepository;
    private final ILlmService llmService;
    private final ObjectMapper objectMapper;

    /**
     * The terminal call of the interview lifecycle.
     *
     * Design:
     *  - If session is still IN_PROGRESS → mark it COMPLETED, generate+save report, return it.
     *  - If session is already COMPLETED and report exists → return the saved report (idempotent).
     *  - If session is already COMPLETED but report is missing → generate+save it, return it.
     *
     * This is the ONLY place where session status transitions to COMPLETED.
     * There is no separate "complete session" endpoint.
     */
    @Override
    @Transactional
    public FeedbackReportResponse getFeedbackReport(Long sessionId, Long userId) {
        log.info("GET /feedback called for session: {}, user: {}", sessionId, userId);

        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found or access denied: " + sessionId));

        if (session.getStatus() == SessionStatus.ABANDONED) {
            throw new IllegalStateException("Cannot generate feedback for an abandoned session.");
        }

        // --- Close the session if it hasn't been closed yet ---
        if (session.getStatus() == SessionStatus.IN_PROGRESS) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);
            log.info("Session {} marked COMPLETED via feedback endpoint.", sessionId);
        }

        // --- Return saved report if it already exists (idempotent re-fetch) ---
        return feedbackReportRepository.findBySessionId(sessionId)
                .map(existing -> {
                    log.info("Returning existing FeedbackReport for session {}.", sessionId);
                    return buildResponse(existing, sessionId);
                })
                .orElseGet(() -> generateAndSave(session, sessionId, userId));
    }

    // =====================================================================
    // Internal: generate + save
    // =====================================================================

    private FeedbackReportResponse generateAndSave(InterviewSession session, Long sessionId, Long userId) {
        log.info("Generating FeedbackReport for session {}.", sessionId);

        // Load all answered questions + batch-fetch scores (no N+1)
        List<SessionQuestion> questions =
                sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        List<Long> questionIds = questions.stream()
                .map(SessionQuestion::getId)
                .collect(Collectors.toList());

        Map<Long, QuestionScore> scoreMap = scoreRepository
                .findBySessionQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(s -> s.getSessionQuestion().getId(), s -> s));

        List<QuestionScore> scores = new ArrayList<>(scoreMap.values());

        // Compute category averages + weighted overall
        int clarity = 0, confidence = 0, pacing = 0, fillerWords = 0, contentRelevance = 0, overall = 0;
        if (!scores.isEmpty()) {
            clarity          = avg(scores, QuestionScore::getClarity);
            confidence       = avg(scores, QuestionScore::getConfidence);
            pacing           = avg(scores, QuestionScore::getPacing);
            fillerWords      = avg(scores, QuestionScore::getFillerWords);
            contentRelevance = avg(scores, QuestionScore::getContentRelevance);
            // Weighted: content(40%) clarity(20%) confidence(20%) pacing(10%) filler(10%)
            overall = (contentRelevance * 40 + clarity * 20 + confidence * 20 + pacing * 10 + fillerWords * 10) / 100;
        }

        // Persist overall score back to the session row
        session.setOverallScore(overall);
        sessionRepository.save(session);

        // Build and persist the FeedbackReport
        FeedbackReport report = new FeedbackReport();
        report.setSession(session);
        report.setOverallScore(overall);
        report.setClarityScore(clarity);
        report.setConfidenceScore(confidence);
        report.setPacingScore(pacing);
        report.setFillerWordsScore(fillerWords);
        report.setContentRelevanceScore(contentRelevance);
        report.setGeneratedAt(LocalDateTime.now());
        report.setCreatedAt(LocalDateTime.now());

        // Generate overall coaching tips via LLM
        try {
            List<String> tips = llmService.generateSessionTips(sessionId, userId);
            report.setCoachingTips(objectMapper.writeValueAsString(tips));
        } catch (Exception e) {
            log.error("Failed to generate session tips for {}: {}", sessionId, e.getMessage());
            report.setCoachingTips("[]");
        }

        FeedbackReport saved = feedbackReportRepository.save(report);
        log.info("Saved FeedbackReport ID: {} (overall: {}) for session {}.",
                saved.getId(), saved.getOverallScore(), sessionId);

        return buildResponse(saved, sessionId);
    }

    // =====================================================================
    // Mapper
    // =====================================================================

    private FeedbackReportResponse buildResponse(FeedbackReport report, Long sessionId) {
        List<SessionQuestion> questions =
                sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        List<Long> qIds = questions.stream().map(SessionQuestion::getId).collect(Collectors.toList());
        Map<Long, QuestionScore> scoreMap = scoreRepository.findBySessionQuestionIdIn(qIds)
                .stream()
                .collect(Collectors.toMap(s -> s.getSessionQuestion().getId(), s -> s));

        List<SessionQuestionResponse> qResponses = questions.stream()
                .map(sq -> {
                    QuestionScore score = scoreMap.get(sq.getId());
                    QuestionScoreResponse scoreResp = score == null ? null : QuestionScoreResponse.builder()
                            .id(score.getId())
                            .sessionQuestionId(score.getSessionQuestion().getId())
                            .contentRelevance(score.getContentRelevance())
                            .clarity(score.getClarity())
                            .confidence(score.getConfidence())
                            .pacing(score.getPacing())
                            .fillerWords(score.getFillerWords())
                            .overallScore(score.getOverallScore())
                            .coachingTip(score.getCoachingTip())
                            .createdAt(score.getCreatedAt())
                            .build();
                    return SessionQuestionResponse.builder()
                            .id(sq.getId())
                            .sessionId(sq.getSession().getId())
                            .questionText(sq.getQuestionText())
                            .questionOrder(sq.getQuestionOrder())
                            .userTranscript(sq.getUserTranscript())
                            .durationMs(sq.getDurationMs())
                            .speechRateWpm(sq.getSpeechRateWpm())
                            .avgPauseMs(sq.getAvgPauseMs())
                            .silenceRatio(sq.getSilenceRatio())
                            .createdAt(sq.getCreatedAt())
                            .completedAt(sq.getCompletedAt())
                            .score(scoreResp)
                            .build();
                })
                .collect(Collectors.toList());

        return FeedbackReportResponse.builder()
                .id(report.getId())
                .sessionId(report.getSession().getId())
                .overallScore(report.getOverallScore())
                .clarityScore(report.getClarityScore())
                .confidenceScore(report.getConfidenceScore())
                .pacingScore(report.getPacingScore())
                .fillerWordsScore(report.getFillerWordsScore())
                .contentRelevanceScore(report.getContentRelevanceScore())
                .coachingTips(parseTips(report.getCoachingTips()))
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .questions(qResponses)
                .build();
    }

    // =====================================================================
    // Utilities
    // =====================================================================

    @FunctionalInterface
    private interface ScoreExtractor {
        int get(QuestionScore s);
    }

    private int avg(List<QuestionScore> scores, ScoreExtractor fn) {
        return (int) Math.round(scores.stream().mapToInt(fn::get).average().orElse(0));
    }

    private List<String> parseTips(String raw) {
        if (raw == null || raw.isBlank()) return new ArrayList<>();
        try {
            if (raw.trim().startsWith("[")) {
                return objectMapper.readValue(raw, new TypeReference<List<String>>() {});
            }
            return List.of(raw);
        } catch (Exception e) {
            log.warn("Failed to parse coaching tips: {}", raw);
            return List.of(raw);
        }
    }
}
