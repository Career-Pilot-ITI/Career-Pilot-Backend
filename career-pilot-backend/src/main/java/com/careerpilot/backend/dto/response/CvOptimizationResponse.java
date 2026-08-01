package com.careerpilot.backend.dto.response;

import java.util.List;

public record CvOptimizationResponse(
    String optimizedCv,
    List<String> recommendedTracks,
    int coinCost
) {

  public static CvOptimizationResponse from(CvOptimization optimization, int coinCost) {
    return new CvOptimizationResponse(
        optimization.optimizedCv(),
        optimization.recommendedTracks(),
        coinCost);
  }
}
