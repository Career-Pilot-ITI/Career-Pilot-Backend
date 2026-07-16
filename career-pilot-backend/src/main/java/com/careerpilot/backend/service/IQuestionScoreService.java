package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.QuestionScoreResponse;
import com.careerpilot.backend.entity.SessionQuestion;

public interface IQuestionScoreService {

    /**
     * Score a session question answer.
     * Computes pacing from word timings (arithmetic), calls LLM for text scores,
     * computes weighted overall score, stores result.
     *
     * @param sessionQuestion the session question with transcript and word timings populated
     * @return the stored QuestionScoreResponse
     */
    QuestionScoreResponse scoreAnswer(SessionQuestion sessionQuestion);

    /**
     * Get an existing score for a session question.
     *
     * @param sessionQuestionId the session question ID
     * @return the QuestionScoreResponse, or null if not scored yet
     */
    QuestionScoreResponse getScore(Long sessionQuestionId);
}
