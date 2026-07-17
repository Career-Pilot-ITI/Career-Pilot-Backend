package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.service.IQuestionScoreService;
import com.careerpilot.backend.service.ISessionQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionQuestionService implements ISessionQuestionService {

    private final ISessionQuestionRepository questionRepository;
    private final IInterviewSessionRepository sessionRepository;
    private final IQuestionBankRepository questionBankRepository;
    private final IQuestionScoreService scoreService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<SessionQuestion> createSessionQuestions(InterviewSession session, List<GeneratedQuestion> generated) {
        log.info("Creating {} session questions for session ID: {}", generated.size(), session.getId());

        List<SessionQuestion> sessionQuestions = IntStream.range(0, generated.size())
                .mapToObj(i -> {
                    GeneratedQuestion gq = generated.get(i);
                    QuestionBank sourceQ = questionBankRepository.findById(gq.sourceQuestionId())
                            .orElse(null);
                    return SessionQuestion.builder()
                            .session(session)
                            .question(sourceQ)
                            .questionText(gq.text())
                            .questionOrder(i + 1)
                            .generatedByLlm(true)
                            .build();
                })
                .collect(Collectors.toList());

        List<SessionQuestion> saved = questionRepository.saveAll(sessionQuestions);
        log.info("Created {} session questions for session ID: {}", saved.size(), session.getId());
        return saved;
    }

    @Override
    @Transactional
    public SessionQuestionResponse submitAnswer(Long sessionId, Long questionId,
                                                SubmitAnswerRequest request, Long userId) {
        log.info("Submitting answer for session: {}, question: {}, user: {}", sessionId, questionId, userId);

        // 1. Validate session exists and belongs to user
        InterviewSession session = sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        // 2. Validate session is still active
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.ABANDONED) {
            throw new IllegalStateException(
                    "Cannot submit answer to a " + session.getStatus().name().toLowerCase() + " session.");
        }

        // 3. Find the session question
        SessionQuestion sq = questionRepository.findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new RuntimeException(
                        "Question " + questionId + " not found in session " + sessionId));

        // 4. Prevent re-submission
        if (sq.getCompletedAt() != null) {
            throw new IllegalStateException("Answer already submitted for question: " + questionId);
        }

        // 5. Serialize word timings to JSON
        String wordTimingsJson = null;
        if (request.getWords() != null && !request.getWords().isEmpty()) {
            try {
                wordTimingsJson = objectMapper.writeValueAsString(request.getWords());
            } catch (Exception e) {
                log.warn("Failed to serialize word timings: {}", e.getMessage());
            }
        }

        // 6. Compute pacing metrics from word timings
        double speechRateWpm = 0;
        double avgPauseMs = 0;
        double silenceRatio = 0;

        List<SubmitAnswerRequest.WordTimingDto> words = request.getWords();
        if (words != null && !words.isEmpty() && request.getDurationMs() != null && request.getDurationMs() > 0) {
            // speech rate: words per minute
            double durationMinutes = request.getDurationMs() / 60_000.0;
            speechRateWpm = words.size() / durationMinutes;

            // avg pause between consecutive words
            if (words.size() > 1) {
                long totalPauseMs = 0;
                for (int i = 1; i < words.size(); i++) {
                    long pause = words.get(i).getStartMs() - words.get(i - 1).getEndMs();
                    if (pause > 0) totalPauseMs += pause;
                }
                avgPauseMs = (double) totalPauseMs / (words.size() - 1);

                // silence ratio: total pause / total duration
                silenceRatio = (double) totalPauseMs / request.getDurationMs();
            }
        }

        // 7. Persist transcript + timings + pacing + audio ref
        sq.setUserTranscript(request.getTranscript());
        sq.setDurationMs(request.getDurationMs());
        sq.setWordTimingsJson(wordTimingsJson);
        sq.setAudioUrl(request.getAudioUrl());
        sq.setSpeechRateWpm(speechRateWpm);
        sq.setAvgPauseMs(avgPauseMs);
        sq.setSilenceRatio(silenceRatio);
        sq.setCompletedAt(LocalDateTime.now());

        SessionQuestion saved = questionRepository.save(sq);
        log.info("Answer stored for question ID: {} — speechRate: {} wpm, silenceRatio: {}", 
                questionId, String.format("%.1f", speechRateWpm), String.format("%.3f", silenceRatio));

        // 8. Checkpoint session (mark still IN_PROGRESS, touch updated timestamp via save)
        sessionRepository.save(session);

        // 9. Trigger synchronous scoring (async can be added later via @Async + event)
        QuestionScoreResponse scoreResponse = null;
        try {
            scoreResponse = scoreService.scoreAnswer(saved);
            log.info("Scoring completed for question ID: {} — overall: {}", questionId,
                    scoreResponse != null ? scoreResponse.getOverallScore() : "N/A");
        } catch (Exception e) {
            log.error("Scoring failed for question ID: {}. Will retry later. Error: {}", questionId, e.getMessage());
        }

        SessionQuestionResponse response = mapToResponse(saved);
        response.setScore(scoreResponse);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionQuestionResponse> getSessionQuestions(Long sessionId, Long userId) {
        // Validate session belongs to user
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        return questionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId)
                .stream()
                .map(sq -> {
                    SessionQuestionResponse resp = mapToResponse(sq);
                    resp.setScore(scoreService.getScore(sq.getId()));
                    return resp;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionQuestionResponse getSessionQuestion(Long sessionId, Long questionId, Long userId) {
        // Validate session belongs to user
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        SessionQuestion sq = questionRepository.findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new RuntimeException(
                        "Question " + questionId + " not found in session " + sessionId));

        SessionQuestionResponse resp = mapToResponse(sq);
        resp.setScore(scoreService.getScore(sq.getId()));
        return resp;
    }

    // ---- Mapper ----

    private SessionQuestionResponse mapToResponse(SessionQuestion sq) {
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
                .build();
    }
}
