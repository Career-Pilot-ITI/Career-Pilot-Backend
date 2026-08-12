package com.careerpilot.backend.dto.response;

import java.util.List;

public record CvOptimizationResponse(
    List<CvSection> sections,
    List<String> recommendedTracks,
    int coinCost
) {
}
