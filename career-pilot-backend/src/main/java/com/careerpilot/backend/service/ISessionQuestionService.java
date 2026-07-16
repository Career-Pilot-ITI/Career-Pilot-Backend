package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;

import java.util.List;

public interface ISessionQuestionService {

    /**
     * Submit an answer for a session question.
     * Stores transcript + word timings, computes pacing metrics, triggers scoring.
     *
     * @param sessionId  the interview session ID
     * @param questionId the session question ID
     * @param request    the answer payload (transcript + word timings)
     * @param userId     the authenticated user ID (for validation)
     * @return the updated SessionQuestion response (with pacing data; score arrives asynchronously)
     */
    SessionQuestionResponse submitAnswer(Long sessionId, Long questionId, SubmitAnswerRequest request, Long userId);

    /**
     * Get all questions for a given session.
     *
     * @param sessionId the session ID
     * @param userId    the authenticated user ID
     * @return ordered list of session questions
     */
    List<SessionQuestionResponse> getSessionQuestions(Long sessionId, Long userId);

    /**
     * Get a specific session question.
     *
     * @param sessionId  the session ID
     * @param questionId the question ID
     * @param userId     the authenticated user ID
     * @return the session question response
     */
    SessionQuestionResponse getSessionQuestion(Long sessionId, Long questionId, Long userId);
}
