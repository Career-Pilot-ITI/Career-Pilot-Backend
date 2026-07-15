package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuestionRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    @NotBlank(message = "Difficulty level is required")
    private String difficultyLevel;

    @NotBlank(message = "Category is required")
    private String category;

    private String expectedKeywords;

    private Boolean isActive;
}
