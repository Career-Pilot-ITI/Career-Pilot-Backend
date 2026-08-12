package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTrackRequest(@NotBlank(message = "name is required") String name, String description) {
}