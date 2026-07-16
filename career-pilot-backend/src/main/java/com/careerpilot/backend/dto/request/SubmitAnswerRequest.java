package com.careerpilot.backend.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for POST /api/v1/interviews/sessions/{id}/questions/{qId}/answer
 * Contains transcript + word timings from mobile STT, no audio file needed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotBlank(message = "Transcript must not be blank")
    private String transcript;

    @NotNull(message = "Duration is required")
    @Min(value = 0, message = "Duration must be non-negative")
    private Long durationMs;

    @Valid
    private List<WordTimingDto> words;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordTimingDto {
        @NotBlank(message = "Word must not be blank")
        private String word;

        @NotNull(message = "startMs is required")
        @Min(value = 0, message = "startMs must be non-negative")
        private Long startMs;

        @NotNull(message = "endMs is required")
        @Min(value = 0, message = "endMs must be non-negative")
        private Long endMs;
    }
}
