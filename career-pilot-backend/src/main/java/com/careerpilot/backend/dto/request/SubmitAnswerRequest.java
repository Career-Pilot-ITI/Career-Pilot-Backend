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
 * Request body for POST /api/v1/interviews/sessions/{sessionId}/answer
 *
 * sessionElapsedSeconds: How many seconds have elapsed on the CLIENT device since the
 * interview started (i.e. since the frontend received the startSession response).
 * This is the authoritative clock for session completion — the backend never trusts its
 * own wall clock for this, because network delay and congestion would make it unreliable.
 *
 * durationMs / words: Optional STT pacing data for this specific answer only.
 * audioUrl: Reserved for future audio-based scoring.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequest {

    @NotBlank(message = "Transcript must not be blank")
    private String transcript;

    /**
     * Client-side elapsed time (seconds) since the interview began.
     * Used as the authoritative source for time-based session completion.
     * If omitted, the backend falls back to the maxQuestions safety cap only.
     */
    @Min(value = 0, message = "sessionElapsedSeconds must be non-negative")
    private Long sessionElapsedSeconds;

    @Min(value = 0, message = "Duration must be non-negative")
    private Long durationMs;   // duration of this specific answer only

    private String audioUrl;

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
