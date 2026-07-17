package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.service.IQuestionScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scoring pipeline for interview answers.
 *
 * Pipeline:
 *   SessionQuestion (transcript + pre-computed pacing stored by SessionQuestionService)
 *     │
 *     ├─ Read pacing score from sq.speechRateWpm (already computed + saved — no re-parsing)
 *     │
 *     └─→ LlmService.scoreAnswer(questionId, userId, transcript)
 *          → {contentRelevance, clarity, confidence, fillerWords}
 *          │
 *          └─ overall = content(40%) + clarity(20%) + confidence(20%) + pacing(10%) + filler(10%)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionScoreService implements IQuestionScoreService {

    private static final int OPTIMAL_WPM_LOW  = 110;
    private static final int OPTIMAL_WPM_HIGH = 160;

    private final IQuestionScoreRepository scoreRepository;
    private final ILlmService llmService;

    @Override
    @Transactional
    public QuestionScoreResponse scoreAnswer(SessionQuestion sessionQuestion) {
        log.info("Scoring answer for session question ID: {}", sessionQuestion.getId());

        // Skip if already scored
        if (scoreRepository.existsBySessionQuestionId(sessionQuestion.getId())) {
            log.info("Answer already scored for session question ID: {}", sessionQuestion.getId());
            return getScore(sessionQuestion.getId());
        }

        // 1. Compute pacing score from the already-saved speechRateWpm field (no re-parsing)
        int pacingScore = computePacingScore(sessionQuestion);
        log.debug("Pacing score: {} for question ID: {}", pacingScore, sessionQuestion.getId());

        // 2. Call LLM with questionId + userId so the impl can load RAG context
        Long questionId = sessionQuestion.getQuestion() != null
                ? sessionQuestion.getQuestion().getId() : null;
        Long userId = sessionQuestion.getSession().getUser().getId();

        ScoreResponse llmResult = llmService.scoreAnswer(questionId, userId,
                sessionQuestion.getUserTranscript());
        log.debug("LLM scores — content:{}, clarity:{}, confidence:{}, filler:{}",
                llmResult.contentRelevance(), llmResult.clarity(),
                llmResult.confidence(), llmResult.fillerWords());

        // 3. Build and save QuestionScore entity
        QuestionScore score = QuestionScore.builder()
                .sessionQuestion(sessionQuestion)
                .contentRelevance(llmResult.contentRelevance())
                .clarity(llmResult.clarity())
                .confidence(llmResult.confidence())
                .fillerWords(llmResult.fillerWords())
                .pacing(pacingScore)
                .overallScore(0)  // calculated below
                .build();

        score.calculateOverallScore();
        QuestionScore saved = scoreRepository.save(score);

        log.info("Saved QuestionScore ID: {} with overall: {}", saved.getId(), saved.getOverallScore());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionScoreResponse getScore(Long sessionQuestionId) {
        return scoreRepository.findBySessionQuestionId(sessionQuestionId)
                .map(this::mapToResponse)
                .orElse(null);
    }

    // ---- Pacing computation ----

    /**
     * Compute pacing score 0-100 from the pre-computed speechRateWpm saved on SessionQuestion.
     * SessionQuestionService already did the arithmetic — we simply read the field here.
     *
     * Optimal speech rate: 110–160 wpm → score 100.
     * Too fast (>250 wpm) or too slow (<30 wpm) → score approaches 0.
     * Returns 50 if no speech rate data is available.
     */
    private int computePacingScore(SessionQuestion sq) {
        Double wpm = sq.getSpeechRateWpm();
        if (wpm == null || wpm <= 0) {
            return 50; // no timing data — neutral default
        }

        int score;
        if (wpm >= OPTIMAL_WPM_LOW && wpm <= OPTIMAL_WPM_HIGH) {
            score = 100;
        } else if (wpm < OPTIMAL_WPM_LOW) {
            // Too slow: linear drop, 0 at 30 wpm
            score = (int) Math.max(0, 100.0 * (wpm - 30) / (OPTIMAL_WPM_LOW - 30));
        } else {
            // Too fast: linear drop, 0 at 250 wpm
            score = (int) Math.max(0, 100.0 * (250 - wpm) / (250 - OPTIMAL_WPM_HIGH));
        }

        return Math.max(0, Math.min(100, score));
    }

    // ---- Mapper ----

    private QuestionScoreResponse mapToResponse(QuestionScore score) {
        return QuestionScoreResponse.builder()
                .id(score.getId())
                .sessionQuestionId(score.getSessionQuestion().getId())
                .contentRelevance(score.getContentRelevance())
                .clarity(score.getClarity())
                .confidence(score.getConfidence())
                .pacing(score.getPacing())
                .fillerWords(score.getFillerWords())
                .overallScore(score.getOverallScore())
                .createdAt(score.getCreatedAt())
                .build();
    }
}
