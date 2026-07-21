package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.FeedbackReportResponse;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.dto.response.SessionStateResponse;
import com.careerpilot.backend.dto.response.StartSessionResponse;
import com.careerpilot.backend.dto.response.SubmitAnswerResponse;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IFeedbackReportService;
import com.careerpilot.backend.service.IInterviewSessionService;
import com.careerpilot.backend.service.ISessionQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/interviews/sessions")
@RequiredArgsConstructor
@Tag(name = "Interview Sessions", description = "Interview lifecycle: start → answer loop → feedback")
@SecurityRequirement(name = "bearerAuth")
public class InterviewSessionController {

    private final IInterviewSessionService sessionService;
    private final ISessionQuestionService sessionQuestionService;
    private final IFeedbackReportService feedbackReportService;

    // =====================================================================
    // SESSION LIFECYCLE
    // =====================================================================

    /**
     * POST /api/v1/interviews/sessions
     *
     * Start a new interview session. Returns the session ID, timer config, and the
     * first dynamically generated open-ended question.
     *
     * The frontend should:
     *   1. Start a client-side timer using targetDurationMinutes.
     *   2. Display currentQuestion to the candidate.
     *   3. Submit answers to POST /sessions/{id}/answer, sending sessionElapsedSeconds
     *      on each submission.
     */
    @PostMapping
    @Operation(
            summary = "Start an interview session",
            description = "Creates a new IN_PROGRESS session and generates the first open-ended question via LLM."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session started",
        content = @Content(schema = @Schema(implementation = StartSessionResponse.class)))
    public ResponseEntity<ApiResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        StartSessionResponse session = sessionService.startSession(request, getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .success(true)
                .message("Interview session started")
                .data(session)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * POST /api/v1/interviews/sessions/{sessionId}/answer
     *
     * Submit an answer to the current active question. The backend:
     *   1. Saves the transcript and pacing metrics.
     *   2. Scores the answer via LLM.
     *   3. Generates the next question dynamically (follow-up or new topic).
     *
     * Response:
     *   - score:          evaluation of the just-submitted answer
     *   - nextQuestion:   null when the session should end
     *   - sessionStatus:  IN_PROGRESS | READY_TO_COMPLETE
     *
     * When sessionStatus == READY_TO_COMPLETE, the frontend should navigate to
     * GET /sessions/{sessionId}/feedback to close the session and retrieve the report.
     */
    @PostMapping("/{sessionId}/answer")
    @Operation(
            summary = "Submit an answer to the current question",
            description = "Stores response, scores it, and generates the next question. " +
                          "Returns READY_TO_COMPLETE when the interview is done — frontend must then call GET /feedback."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Answer processed",
        content = @Content(schema = @Schema(implementation = SubmitAnswerResponse.class)))
    public ResponseEntity<ApiResponse> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {

        SubmitAnswerResponse result = sessionService.submitAnswer(sessionId, request, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Answer processed")
                .data(result)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/feedback
     *
     * THE terminal call of the interview lifecycle.
     *
     * - Marks the session COMPLETED (if not already).
     * - Generates and persists the FeedbackReport (LLM coaching tips + score breakdown).
     * - Idempotent: subsequent calls return the already-saved report instantly.
     *
     * Frontend calls this when:
     *   a) sessionStatus == READY_TO_COMPLETE (received from the last answer submit), OR
     *   b) The candidate taps "End Interview" manually at any point.
     */
    @GetMapping("/{sessionId}/feedback")
    @Operation(
            summary = "Get feedback report — also closes the session",
            description = "Terminal endpoint. Transitions session to COMPLETED, generates+saves the feedback " +
                          "report, and returns it. Idempotent."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feedback report",
        content = @Content(schema = @Schema(implementation = FeedbackReportResponse.class)))
    public ResponseEntity<ApiResponse> getFeedbackReport(@PathVariable Long sessionId) {
        FeedbackReportResponse report = feedbackReportService.getFeedbackReport(sessionId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Feedback report ready")
                .data(report)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/state
     *
     * Network-drop recovery endpoint.
     *
     * Returns the current session state: session metadata, answered questions with
     * scores (if available), and the current unanswered question the candidate
     * should answer next. The mobile app calls this after reconnecting to
     * re-render the interview screen without needing multiple round-trips.
     *
     * Works for IN_PROGRESS sessions. Returns 404 if session not found or does
     * not belong to the authenticated user.
     */
    @GetMapping("/{sessionId}/state")
    @Operation(
            summary = "Get session state for network-drop recovery",
            description = "Returns session metadata, answered questions, and the current unanswered question. " +
                          "Call this after reconnecting to re-render the interview without multiple round-trips."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session state",
        content = @Content(schema = @Schema(implementation = SessionStateResponse.class)))
    public ResponseEntity<ApiResponse> getSessionState(@PathVariable Long sessionId) {
        SessionStateResponse state = sessionService.getSessionState(sessionId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session state retrieved")
                .data(state)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // =====================================================================
    // SESSION READ
    // =====================================================================

    /**
     * GET /api/v1/interviews/sessions
     * List all sessions for the authenticated user, newest first (metadata only — no questions).
     */
    @GetMapping
    @Operation(summary = "List user's interview sessions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sessions list",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = InterviewSessionResponse.class))))
    public ResponseEntity<ApiResponse> listSessions() {
        List<InterviewSessionResponse> sessions = sessionService.listSessions(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Sessions retrieved")
                .data(sessions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}
     * Session metadata (status, scores, timestamps). No embedded questions.
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session detail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session detail",
        content = @Content(schema = @Schema(implementation = InterviewSessionResponse.class)))
    public ResponseEntity<ApiResponse> getSession(@PathVariable Long sessionId) {
        InterviewSessionResponse session = sessionService.getSession(sessionId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session retrieved")
                .data(session)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // =====================================================================
    // QUESTION READ (for debugging / admin views)
    // =====================================================================

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/questions
     * All questions with scores — primarily used by the feedback view.
     */
    @GetMapping("/{sessionId}/questions")
    @Operation(summary = "List all questions and scores for a session")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session questions",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = SessionQuestionResponse.class))))
    public ResponseEntity<ApiResponse> getSessionQuestions(@PathVariable Long sessionId) {
        List<SessionQuestionResponse> questions =
                sessionQuestionService.getSessionQuestions(sessionId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions retrieved")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/questions/{questionId}
     * Single question with its score.
     */
    @GetMapping("/{sessionId}/questions/{questionId}")
    @Operation(summary = "Get a specific session question")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session question",
        content = @Content(schema = @Schema(implementation = SessionQuestionResponse.class)))
    public ResponseEntity<ApiResponse> getSessionQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId) {

        SessionQuestionResponse question =
                sessionQuestionService.getSessionQuestion(sessionId, questionId, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Question retrieved")
                .data(question)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // =====================================================================
    // Helper
    // =====================================================================

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((CustomUserDetails) auth.getPrincipal()).getUser().getId();
    }
}
