package com.careerpilot.backend.dto.response;

import java.util.List;

public record CvSection(
    String name,
    int score,
    List<CvSectionImprovement> improvements
) {
    public CvSection {
        improvements = improvements != null ? improvements : List.of();
    }
}
