package com.careerpilot.backend.dto.response;

public record SectionScore(
    String section,
    int score,
    String feedback
) {}
