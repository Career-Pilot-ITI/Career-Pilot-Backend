package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotNull(message = "Track ID is required")
    private Long trackId;

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Difficulty level is required")
    private String difficultyLevel; // EASY, MEDIUM, HARD

    @NotBlank(message = "Category is required")
    private String category;

    private String expectedKeywords; // JSON array or comma-separated
}

