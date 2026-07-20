package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.FeedbackReportResponse;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.service.IFeedbackReportService;
import com.careerpilot.backend.service.IQuestionScoreService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackReportService implements IFeedbackReportService {

    private final IFeedbackReportRepository feedbackReportRepository;
    private final IInterviewSessionRepository sessionRepository;
    private final ISessionQuestionRepository sessionQuestionRepository;
    private final IQuestionScoreRepository scoreRepository;
    private final IQuestionScoreService scoreService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public FeedbackReportResponse getFeedbackReport(Long sessionId, Long userId) {
        log.info("Fetching feedback report for session ID: {}, user ID: {}", sessionId, userId);

        // 1. Verify session exists and belongs to user
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found or access denied: " + sessionId));

        // 2. Fetch existing report or generate if not present
        Optional<FeedbackReport> existingReport = feedbackReportRepository.findBySessionId(sessionId);
        if (existingReport.isPresent()) {
            return mapToResponse(existingReport.get(), sessionId);
        }

        // Lazy generation if not present
        return generateFeedbackReport(sessionId, userId);
    }

    @Override
    @Transactional
    public FeedbackReportResponse generateFeedbackReport(Long sessionId, Long userId) {
        log.info("Generating feedback report for session ID: {}, user ID: {}", sessionId, userId);

        // 1. Load session with security check
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found or access denied: " + sessionId));

        // 2. Load questions and scores for this session
        List<SessionQuestion> questions = sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        List<QuestionScore> scores = questions.stream()
                .map(sq -> scoreRepository.findBySessionQuestionId(sq.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 3. Compute category averages
        int clarityAvg = 0;
        int confidenceAvg = 0;
        int pacingAvg = 0;
        int fillerWordsAvg = 0;
        int contentRelevanceAvg = 0;
        int overallScore = 0;

        if (!scores.isEmpty()) {
            clarityAvg = (int) Math.round(scores.stream().mapToInt(QuestionScore::getClarity).average().orElse(0));
            confidenceAvg = (int) Math.round(scores.stream().mapToInt(QuestionScore::getConfidence).average().orElse(0));
            pacingAvg = (int) Math.round(scores.stream().mapToInt(QuestionScore::getPacing).average().orElse(0));
            fillerWordsAvg = (int) Math.round(scores.stream().mapToInt(QuestionScore::getFillerWords).average().orElse(0));
            contentRelevanceAvg = (int) Math.round(scores.stream().mapToInt(QuestionScore::getContentRelevance).average().orElse(0));

            // Weighted average: contentRelevance(40%), clarity(20%), confidence(20%), pacing(10%), fillerWords(10%)
            overallScore = (contentRelevanceAvg * 40 + clarityAvg * 20 + confidenceAvg * 20 + pacingAvg * 10 + fillerWordsAvg * 10) / 100;
        }

        // 4. Update overall score on interview session entity
        session.setOverallScore(overallScore);
        sessionRepository.save(session);

        // 5. Get or create FeedbackReport entity
        FeedbackReport report = feedbackReportRepository.findBySessionId(sessionId)
                .orElseGet(() -> {
                    FeedbackReport r = new FeedbackReport();
                    r.setSession(session);
                    r.setCreatedAt(LocalDateTime.now());
                    return r;
                });

        report.setOverallScore(overallScore);
        report.setClarityScore(clarityAvg);
        report.setConfidenceScore(confidenceAvg);
        report.setPacingScore(pacingAvg);
        report.setFillerWordsScore(fillerWordsAvg);
        report.setContentRelevanceScore(contentRelevanceAvg);
        report.setGeneratedAt(LocalDateTime.now());

        if (report.getCoachingTips() == null) {
            report.setCoachingTips("[]");
        }

        FeedbackReport savedReport = feedbackReportRepository.save(report);
        log.info("Saved FeedbackReport ID: {} with overall score: {}", savedReport.getId(), savedReport.getOverallScore());

        return mapToResponse(savedReport, sessionId);
    }

    private FeedbackReportResponse mapToResponse(FeedbackReport report, Long sessionId) {
        List<SessionQuestion> questions = sessionQuestionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        List<SessionQuestionResponse> questionResponses = questions.stream()
                .map(sq -> {
                    QuestionScoreResponse scoreResp = scoreService.getScore(sq.getId());
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

        List<String> tipsList = parseCoachingTips(report.getCoachingTips());

        return FeedbackReportResponse.builder()
                .id(report.getId())
                .sessionId(report.getSession().getId())
                .overallScore(report.getOverallScore())
                .clarityScore(report.getClarityScore())
                .confidenceScore(report.getConfidenceScore())
                .pacingScore(report.getPacingScore())
                .fillerWordsScore(report.getFillerWordsScore())
                .contentRelevanceScore(report.getContentRelevanceScore())
                .coachingTips(tipsList)
                .generatedAt(report.getGeneratedAt())
                .createdAt(report.getCreatedAt())
                .questions(questionResponses)
                .build();
    }

    private List<String> parseCoachingTips(String rawTips) {
        if (rawTips == null || rawTips.isBlank()) {
            return new ArrayList<>();
        }
        try {
            if (rawTips.trim().startsWith("[")) {
                return objectMapper.readValue(rawTips, new TypeReference<List<String>>() {});
            } else {
                return List.of(rawTips);
            }
        } catch (Exception e) {
            log.warn("Failed to parse coaching tips string: {}", rawTips, e);
            return List.of(rawTips);
        }
    }
}
