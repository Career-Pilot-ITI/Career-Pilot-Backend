package com.careerpilot.backend.dto.response;

import java.util.List;

public record CvAnalysis(
    List<String> skills,
    int yearsOfExperience,
    String targetRole,
    String educationLevel
) {}
