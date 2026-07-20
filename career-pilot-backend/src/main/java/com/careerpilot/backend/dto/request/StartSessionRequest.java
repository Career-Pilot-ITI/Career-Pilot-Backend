package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for POST /api/v1/interviews/sessions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartSessionRequest {

    @NotNull(message = "trackId is required")
    private Long trackId;

    @Min(value = 1, message = "questionCount must be at least 1")
    @Max(value = 50, message = "questionCount must not exceed 50")
    @Builder.Default
    private Integer questionCount = 10;

    @Min(value = 1, message = "durationMinutes must be at least 1")
    @Max(value = 120, message = "durationMinutes must not exceed 120")
    @Builder.Default
    private Integer durationMinutes = 15;
}
