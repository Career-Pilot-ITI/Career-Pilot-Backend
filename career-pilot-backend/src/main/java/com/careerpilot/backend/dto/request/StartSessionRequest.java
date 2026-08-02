package com.careerpilot.backend.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Request body for POST /api/v1/interviews/sessions.
 * Either {@code trackId} or {@code workspaceId} must be provided.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@StartSessionRequest.TrackOrWorkspaceRequired
public class StartSessionRequest {

    /** Explicit track to use. Mutually exclusive with {@code workspaceId}. */
    private Long trackId;

    /** Workspace whose job listing is used to auto-resolve the track. */
    private Long workspaceId;

    @Min(value = 1, message = "questionCount must be at least 1")
    @Max(value = 50, message = "questionCount must not exceed 50")
    @Builder.Default
    private Integer questionCount = 10;

    @Min(value = 1, message = "durationMinutes must be at least 1")
    @Max(value = 120, message = "durationMinutes must not exceed 120")
    @Builder.Default
    private Integer durationMinutes = 15;

    // -------------------------------------------------------------------------
    // Class-level constraint: at least one of trackId / workspaceId is required
    // -------------------------------------------------------------------------

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = TrackOrWorkspaceValidator.class)
    public @interface TrackOrWorkspaceRequired {
        String message() default "Either trackId or workspaceId must be provided";
        Class<?>[] groups() default {};
        Class<? extends Payload>[] payload() default {};
    }

    public static class TrackOrWorkspaceValidator
            implements ConstraintValidator<TrackOrWorkspaceRequired, StartSessionRequest> {

        @Override
        public boolean isValid(StartSessionRequest req, ConstraintValidatorContext ctx) {
            return req.getTrackId() != null || req.getWorkspaceId() != null;
        }
    }
}
