package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.careerpilot.backend.service.ISessionQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionQuestionService implements ISessionQuestionService {

    private final ISessionQuestionRepository questionRepository;
    private final IInterviewSessionRepository sessionRepository;
    private final IQuestionScoreRepository scoreRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SessionQuestionResponse> getSessionQuestions(Long sessionId, Long userId) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        List<SessionQuestion> questions =
                questionRepository.findBySessionIdOrderByQuestionOrderAsc(sessionId);

        // Batch-load all scores in one query — no N+1
        Map<Long, QuestionScore> scoreMap = scoreRepository
                .findBySessionQuestionIdIn(
                        questions.stream().map(SessionQuestion::getId).collect(Collectors.toList()))
                .stream()
                .collect(Collectors.toMap(s -> s.getSessionQuestion().getId(), s -> s));

        return questions.stream()
                .map(sq -> mapToResponse(sq, scoreMap.get(sq.getId())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SessionQuestionResponse getSessionQuestion(Long sessionId, Long questionId, Long userId) {
        sessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        SessionQuestion sq = questionRepository.findByIdAndSessionId(questionId, sessionId)
                .orElseThrow(() -> new RuntimeException(
                        "Question " + questionId + " not found in session " + sessionId));

        QuestionScore score = scoreRepository.findBySessionQuestionId(sq.getId()).orElse(null);
        return mapToResponse(sq, score);
    }

    // ---- Mapper ----

    public static SessionQuestionResponse mapToResponse(SessionQuestion sq, QuestionScore score) {
        QuestionScoreResponse scoreResp = null;
        if (score != null) {
            scoreResp = QuestionScoreResponse.builder()
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
        }
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
    }
}
