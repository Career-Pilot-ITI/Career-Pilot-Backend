package com.careerpilot.backend.dto.response;

import java.time.LocalDateTime;

public record TrackResponse(Long id, String name, String description, boolean isActive, LocalDateTime createdAt) {
}
