package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.StartSessionResponse;
import com.careerpilot.backend.dto.response.SubmitAnswerResponse;

import java.util.List;

public interface IInterviewSessionService {

    /**
     * Start a new interview session for a track.
     * Generates the first open-ended question via LLM and returns it immediately.
     * No questions are pre-generated — each question is produced dynamically
     * after the candidate's previous answer.
     */
    StartSessionResponse startSession(StartSessionRequest request, Long userId);

    /**
     * Submit an answer to the current active (unanswered) question in the session.
     *
     * Returns:
     *  - score:         evaluation of the just-submitted answer
     *  - nextQuestion:  dynamically generated follow-up/next question (null when session ends)
     *  - sessionStatus: IN_PROGRESS | COMPLETED
     *
     * Session completion is determined by the client-reported sessionElapsedSeconds
     * vs targetDurationMinutes, with maxQuestions as a safety cap.
     */
    SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request, Long userId);

    /**
     * List all sessions for the authenticated user, newest first.
     * Does NOT embed questions — frontend uses this for history/dashboard views.
     */
    List<InterviewSessionResponse> listSessions(Long userId);

    /**
     * Get a single session's metadata. Does NOT embed questions.
     */
    InterviewSessionResponse getSession(Long sessionId, Long userId);
}
