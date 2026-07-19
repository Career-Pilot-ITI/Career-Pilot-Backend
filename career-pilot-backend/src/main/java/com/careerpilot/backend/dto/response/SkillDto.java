package com.careerpilot.backend.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Skill with assessment data from interview scoring")
public record SkillDto(
        @Schema(example = "Java") String skillName,
        @Schema(example = "TECHNICAL") String category,
        @Schema(example = "85") Integer performanceScore,
        @Schema(example = "3") Integer timesAssessed,
        LocalDateTime lastAssessedAt) {
}
