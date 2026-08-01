package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CvOptimization(
    String optimizedCv,
    List<String> recommendedTracks
) {

  public CvOptimization {
    recommendedTracks = recommendedTracks != null ? recommendedTracks : List.of();
  }

  public CvOptimization() {
    this("", List.of());
  }
}
