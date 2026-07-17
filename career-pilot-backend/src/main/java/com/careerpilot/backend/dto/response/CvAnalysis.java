package com.careerpilot.backend.dto.response;

import java.util.List;

public record CvAnalysis(
    List<String> skills,
    int yearsOfExperience,
    String targetRole,
    String educationLevel,
    String displayName,
    String currentJobTitle
) {
    public CvAnalysis() {
        this(List.of(), 0, "", "", "", "");
    }
}
