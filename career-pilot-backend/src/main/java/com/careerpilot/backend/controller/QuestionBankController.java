package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.dto.request.CreateQuestionRequest;
import com.careerpilot.backend.dto.request.UpdateQuestionRequest;
import com.careerpilot.backend.dto.response.QuestionResponse;
import com.careerpilot.backend.service.IQuestionBankService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class QuestionBankController {

    private final IQuestionBankService questionService;

    // ========== CREATE ==========

    @PostMapping
    public ResponseEntity<ApiResponse> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request) {
        QuestionResponse question = questionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.builder()
                        .success(true)
                        .message("Question created successfully")
                        .data(question)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    // ========== READ ==========

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getQuestion(@PathVariable Long id) {
        QuestionResponse question = questionService.getQuestion(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Question retrieved successfully")
                .data(question)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse> getAllQuestions(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<QuestionResponse> questions = questionService.getAllQuestionsPaginated(pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions retrieved successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/by-track/{trackId}")
    public ResponseEntity<ApiResponse> getQuestionsByTrack(
            @PathVariable Long trackId,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionResponse> questions = questionService.getQuestionsByTrack(trackId, pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions retrieved by track successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/by-difficulty/{difficulty}")
    public ResponseEntity<ApiResponse> getQuestionsByDifficulty(
            @PathVariable String difficulty,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionResponse> questions = questionService.getQuestionsByDifficulty(difficulty, pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions retrieved by difficulty successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/by-category/{category}")
    public ResponseEntity<ApiResponse> getQuestionsByCategory(
            @PathVariable String category,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionResponse> questions = questionService.getQuestionsByCategory(category, pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions retrieved by category successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse> searchQuestions(
            @RequestParam String text,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<QuestionResponse> questions = questionService.searchQuestions(text, pageable);

        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Questions searched successfully")
                .data(questions)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ========== UPDATE ==========

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest request) {

        QuestionResponse question = questionService.updateQuestion(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Question updated successfully")
                .data(question)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse> toggleQuestionStatus(@PathVariable Long id) {
        QuestionResponse question = questionService.toggleQuestionStatus(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Question status toggled successfully")
                .data(question)
                .timestamp(LocalDateTime.now())
                .build());
    }

    // ========== DELETE ==========

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Question deleted successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/track/{trackId}")
    public ResponseEntity<ApiResponse> deleteQuestionsByTrack(@PathVariable Long trackId) {
        questionService.deleteQuestionsByTrack(trackId);
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("All questions for track deleted successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }
}