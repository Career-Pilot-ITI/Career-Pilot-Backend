package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.entity.SessionQuestion;

public interface IQuestionScoreService {

    /**
     * Score a session question's answer.
     * Computes pacing from word timings, calls LLM for text scores,
     * computes weighted overall score, and persists the result.
     * Idempotent — returns the existing score if already scored.
     *
     * @param sessionQuestion the question with transcript and pacing fields populated
     * @return the stored QuestionScoreResponse
     */
    QuestionScoreResponse scoreAnswer(SessionQuestion sessionQuestion);

    /**
     * Retrieve a previously-saved score for a session question.
     *
     * @param sessionQuestionId the session question's ID
     * @return the score response, or null if not yet scored
     */
    QuestionScoreResponse getScore(Long sessionQuestionId);
}
