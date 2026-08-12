package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTrackRequest(@NotBlank(message = "name is required") String name, String description,
                                 Boolean active) {
}