package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.dto.request.SubmitAnswerRequest;
import com.careerpilot.backend.dto.response.SessionQuestionResponse;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.ISessionQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for interview session question operations.
 *
 * Base path: /api/v1/interviews/sessions/{sessionId}/questions
 */
@RestController
@RequestMapping("/api/v1/interviews/sessions/{sessionId}/questions")
@RequiredArgsConstructor
@Tag(name = "Interview Session Questions", description = "Submit answers and view session questions")
@SecurityRequirement(name = "bearerAuth")
public class InterviewSessionController {

    private final ISessionQuestionService sessionQuestionService;


    @GetMapping
    @Operation(summary = "List all questions for a session")
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
     * Get a specific session question (with score if available).
     */
    @GetMapping("/{questionId}")
    @Operation(summary = "Get a specific session question")
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


    @PostMapping("/{questionId}/answer")
    @Operation(
            summary = "Submit an answer for a session question",
            description = "Stores transcript + word timings, computes pacing metrics server-side, and triggers LLM scoring. " +
                    "Cannot submit to a COMPLETED or ABANDONED session."
    )
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


    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userDetails.getUser().getId();
    }
}
