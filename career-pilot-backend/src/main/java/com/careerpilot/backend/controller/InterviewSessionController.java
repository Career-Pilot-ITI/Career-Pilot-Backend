package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.dto.request.StartSessionRequest;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.FeedbackReportResponse;
import com.careerpilot.backend.dto.response.InterviewSessionResponse;
import com.careerpilot.backend.dto.response.ResumeSessionResponse;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
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
@Tag(name = "Interview Sessions", description = "Session lifecycle and question answer submission")
@SecurityRequirement(name = "bearerAuth")
public class InterviewSessionController {

    private final IInterviewSessionService sessionService;
    private final ISessionQuestionService sessionQuestionService;
    private final IFeedbackReportService feedbackReportService;

    // ===================== Session Management (#38) =====================

    /**
     * POST /api/v1/interviews/sessions
     * Start a new interview session for a given track.
     * Selects up to 5 active questions from the track's question bank.
     */
    @PostMapping
    @Operation(
            summary = "Start an interview session",
            description = "Creates a new IN_PROGRESS session for the given track and generates {questionCount} LLM-powered interview questions."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Session created",
        content = @Content(schema = @Schema(implementation = InterviewSessionResponse.class)))
    public ResponseEntity<ApiResponse> startSession(@Valid @RequestBody StartSessionRequest request) {
        Long userId = getCurrentUserId();
        InterviewSessionResponse session = sessionService.startSession(request, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .success(true)
                .message("Interview session started successfully")
                .data(session)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions
     * List all sessions for the authenticated user, newest first.
     */
    @GetMapping
    @Operation(summary = "List user's interview sessions")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sessions list",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = InterviewSessionResponse.class))))
    public ResponseEntity<ApiResponse> listSessions() {
        Long userId = getCurrentUserId();
        List<InterviewSessionResponse> sessions = sessionService.listSessions(userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Sessions retrieved successfully")
                .data(sessions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}
     * Get a session's detail including its questions (and scores if available).
     */
    @GetMapping("/{sessionId}")
    @Operation(summary = "Get session detail")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session detail",
        content = @Content(schema = @Schema(implementation = InterviewSessionResponse.class)))
    public ResponseEntity<ApiResponse> getSession(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSessionResponse session = sessionService.getSession(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session retrieved successfully")
                .data(session)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * PATCH /api/v1/interviews/sessions/{sessionId}/complete
     * Mark a session as COMPLETED. Records completedAt and duration.
     */
    @PatchMapping("/{sessionId}/complete")
    @Operation(summary = "Complete an interview session")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session completed",
        content = @Content(schema = @Schema(implementation = InterviewSessionResponse.class)))
    public ResponseEntity<ApiResponse> completeSession(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        InterviewSessionResponse session = sessionService.completeSession(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session completed successfully")
                .data(session)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ===================== Session Resume (#41) =====================

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/resume
     * Resume a session after a network drop.
     * Returns session state, answered questions with scores, and the next unanswered question.
     */
    @GetMapping("/{sessionId}/resume")
    @Operation(
            summary = "Resume session after network drop",
            description = "Returns session state, answered questions with scores, and the next unanswered question."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session state with next question",
        content = @Content(schema = @Schema(implementation = ResumeSessionResponse.class)))
    public ResponseEntity<ApiResponse> resumeSession(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        ResumeSessionResponse response = sessionService.resumeSession(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session resumed successfully")
                .data(response)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ===================== Feedback Report (#41) =====================

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/feedback
     * Get full feedback report for a session including overall score, category scores, and per-question breakdown.
     */
    @GetMapping("/{sessionId}/feedback")
    @Operation(
            summary = "Get full feedback report for an interview session",
            description = "Returns structured feedback report: overall score (0-100), 5 category scores, coaching tips, and per-question score breakdown."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Feedback report retrieved successfully",
        content = @Content(schema = @Schema(implementation = FeedbackReportResponse.class)))
    public ResponseEntity<ApiResponse> getFeedbackReport(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        FeedbackReportResponse report = feedbackReportService.getFeedbackReport(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Feedback report retrieved successfully")
                .data(report)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ===================== Question Answer Submission (#39) =====================

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/questions
     * List all questions for a session with their scores (if available).
     */
    @GetMapping("/{sessionId}/questions")
    @Operation(summary = "List all questions for a session")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session questions",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = SessionQuestionResponse.class))))
    public ResponseEntity<ApiResponse> getSessionQuestions(@PathVariable Long sessionId) {
        Long userId = getCurrentUserId();
        List<SessionQuestionResponse> questions =
                sessionQuestionService.getSessionQuestions(sessionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session questions retrieved successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * GET /api/v1/interviews/sessions/{sessionId}/questions/{questionId}
     * Get a specific session question with its score.
     */
    @GetMapping("/{sessionId}/questions/{questionId}")
    @Operation(summary = "Get a specific session question")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Session question",
        content = @Content(schema = @Schema(implementation = SessionQuestionResponse.class)))
    public ResponseEntity<ApiResponse> getSessionQuestion(
            @PathVariable Long sessionId,
            @PathVariable Long questionId) {

        Long userId = getCurrentUserId();
        SessionQuestionResponse question =
                sessionQuestionService.getSessionQuestion(sessionId, questionId, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Session question retrieved successfully")
                .data(question)
                .timestamp(LocalDateTime.now())
                .build());
    }

    /**
     * POST /api/v1/interviews/sessions/{sessionId}/questions/{questionId}/answer
     * Submit an answer. Stores transcript + word timings, computes pacing, triggers LLM scoring.
     * Cannot submit to a COMPLETED or ABANDONED session.
     */
    @PostMapping("/{sessionId}/questions/{questionId}/answer")
    @Operation(
            summary = "Submit an answer for a session question",
            description = "Stores transcript + word timings, computes pacing metrics server-side, and triggers LLM scoring. " +
                    "Cannot submit to a COMPLETED or ABANDONED session."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Answer submitted and scored",
        content = @Content(schema = @Schema(implementation = SessionQuestionResponse.class)))
    public ResponseEntity<ApiResponse> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody SubmitAnswerRequest request) {

        Long userId = getCurrentUserId();
        SessionQuestionResponse result = sessionQuestionService.submitAnswer(
                sessionId, questionId, request, userId);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Answer submitted and scored successfully")
                .data(result)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser().getId();
    }
}
