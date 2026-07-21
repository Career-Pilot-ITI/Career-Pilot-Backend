package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.SessionStateResponse;
import com.careerpilot.backend.dto.response.StartSessionResponse;
import com.careerpilot.backend.dto.response.SubmitAnswerResponse;

import java.util.List;

public interface IInterviewSessionService {

    StartSessionResponse startSession(StartSessionRequest request, Long userId);

    SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request, Long userId);

    List<InterviewSessionResponse> listSessions(Long userId);

    InterviewSessionResponse getSession(Long sessionId, Long userId);

    SessionStateResponse getSessionState(Long sessionId, Long userId);
}
