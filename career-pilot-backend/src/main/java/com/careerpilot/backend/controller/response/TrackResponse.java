package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.Track;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Track response")
public record TrackResponse(
        Long id,
        String name,
        String description,
        boolean isActive,
        LocalDateTime createdAt
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(
                track.getId(),
                track.getName(),
                track.getDescription(),
                track.getIsActive(),
                track.getCreatedAt()
        );
    }
}
