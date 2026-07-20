package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.SessionQuestionResponse;

import java.util.List;

public interface ISessionQuestionService {

    /**
     * Get all questions for a session (with their scores if available).
     * Used by the feedback endpoint and history views.
     */
    List<SessionQuestionResponse> getSessionQuestions(Long sessionId, Long userId);

    /**
     * Get a specific question within a session.
     */
    SessionQuestionResponse getSessionQuestion(Long sessionId, Long questionId, Long userId);
}
