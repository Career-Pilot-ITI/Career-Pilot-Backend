package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.service.IQuestionScoreService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Scoring pipeline for interview answers.
 *
 * Pipeline:
 *   Mobile JSON (transcript + words[])
 *     │
 *     ├─ Compute pacing (pure math):
 *     │    speechRate    = words.length / durationMs * 60000
 *     │    avgPause      = avg(startMs[i+1] - endMs[i])
 *     │    silenceRatio  = totalPauseMs / durationMs
 *     │    pacingScore   = map speechRate to 0-100 (optimal ~120-150 wpm)
 *     │
 *     └─→ LlmService.scoreAnswer(question, transcript)
 *          → {contentRelevance, clarity, confidence, fillerWords}
 *          │
 *          └─ overall = content(40%) + clarity(20%) + confidence(20%) + pacing(10%) + fillerWords(10%)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionScoreService implements IQuestionScoreService {

    private static final int OPTIMAL_WPM_LOW  = 110;
    private static final int OPTIMAL_WPM_HIGH = 160;

    private final IQuestionScoreRepository scoreRepository;
    private final ILlmService llmService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public QuestionScoreResponse scoreAnswer(SessionQuestion sessionQuestion) {
        log.info("Scoring answer for session question ID: {}", sessionQuestion.getId());

        // Skip if already scored
        if (scoreRepository.existsBySessionQuestionId(sessionQuestion.getId())) {
            log.info("Answer already scored for session question ID: {}", sessionQuestion.getId());
            return getScore(sessionQuestion.getId());
        }

        // 1. Compute pacing score from word timings
        int pacingScore = computePacingScore(sessionQuestion);
        log.debug("Computed pacing score: {} for question ID: {}", pacingScore, sessionQuestion.getId());

        // 2. Call LLM for text-based scores
        ILlmService.LlmScoreResult llmResult = llmService.scoreAnswer(
                sessionQuestion.getQuestionText(),
                sessionQuestion.getUserTranscript()
        );
        log.debug("LLM scores — content:{}, clarity:{}, confidence:{}, filler:{}",
                llmResult.contentRelevance(), llmResult.clarity(), llmResult.confidence(), llmResult.fillerWords());

        // 3. Build and save QuestionScore entity
        QuestionScore score = QuestionScore.builder()
                .sessionQuestion(sessionQuestion)
                .contentRelevance(llmResult.contentRelevance())
                .clarity(llmResult.clarity())
                .confidence(llmResult.confidence())
                .fillerWords(llmResult.fillerWords())
                .pacing(pacingScore)
                .overallScore(0)  // will be calculated below
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
     * Compute pacing score 0-100 from word timestamps.
     * Optimal speech rate is ~110–160 wpm → score 100.
     * Too fast (>200 wpm) or too slow (<60 wpm) → score approaches 0.
     *
     * If no word timings are available, returns a neutral score of 50.
     */
    private int computePacingScore(SessionQuestion sq) {
        if (sq.getWordTimingsJson() == null || sq.getWordTimingsJson().isBlank()
                || sq.getDurationMs() == null || sq.getDurationMs() <= 0) {
            return 50; // no timing data available
        }

        try {
            List<SubmitAnswerRequest.WordTimingDto> words = objectMapper.readValue(
                    sq.getWordTimingsJson(),
                    new TypeReference<List<SubmitAnswerRequest.WordTimingDto>>() {}
            );

            if (words == null || words.isEmpty()) {
                return 50;
            }

            double durationMinutes = sq.getDurationMs() / 60_000.0;
            double speechRateWpm = words.size() / durationMinutes;

            // Map to 0-100: parabola centered at optimal range
            int score;
            if (speechRateWpm >= OPTIMAL_WPM_LOW && speechRateWpm <= OPTIMAL_WPM_HIGH) {
                score = 100;
            } else if (speechRateWpm < OPTIMAL_WPM_LOW) {
                // Too slow: linear drop from optimal to 0 at 30 wpm
                score = (int) Math.max(0, 100 * (speechRateWpm - 30) / (OPTIMAL_WPM_LOW - 30));
            } else {
                // Too fast: linear drop from optimal to 0 at 250 wpm
                score = (int) Math.max(0, 100 * (250 - speechRateWpm) / (250 - OPTIMAL_WPM_HIGH));
            }

            return Math.max(0, Math.min(100, score));

        } catch (Exception e) {
            log.warn("Failed to parse word timings for pacing score, using default. Error: {}", e.getMessage());
            return 50;
        }
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
