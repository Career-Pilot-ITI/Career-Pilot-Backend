package com.careerpilot.backend.dto.response;

import java.time.LocalDateTime;

public record SkillDto(String skillName, String category, Integer performanceScore, Integer timesAssessed,
    LocalDateTime lastAssessedAt) {
}
