package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;

import java.util.List;

public interface IInterviewSessionService {

    /**
     * Start a new interview session for a track.
     * Picks up to 5 active questions from the track's question bank and attaches them.
     *
     * @param request contains the trackId to start the session for
     * @param userId  the authenticated user's ID
     * @return the created InterviewSession with its questions
     */
    InterviewSessionResponse startSession(StartSessionRequest request, Long userId);

    /**
     * List all sessions for the authenticated user, newest first.
     *
     * @param userId the authenticated user's ID
     * @return ordered list of InterviewSessionResponse
     */
    List<InterviewSessionResponse> listSessions(Long userId);

    /**
     * Get a single session's details including its questions (with scores if available).
     *
     * @param sessionId the session ID
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the InterviewSessionResponse
     */
    InterviewSessionResponse getSession(Long sessionId, Long userId);

    /**
     * Mark a session as COMPLETED and record its completion time.
     *
     * @param sessionId the session ID
     * @param userId    the authenticated user's ID (for ownership check)
     * @return the updated InterviewSessionResponse
     */
    InterviewSessionResponse completeSession(Long sessionId, Long userId);
}
