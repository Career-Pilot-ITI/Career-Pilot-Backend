package com.careerpilot.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AtsScoreResponse(
    int overallScore,
    int matchPercentage,
    List<String> matchedSkills,
    List<String> missingRequiredSkills,
    List<String> missingPreferredSkills,
    List<String> strengths,
    List<String> weaknesses,
    List<SectionScore> sections,
    List<String> recommendations,
    int coinCost,
    LocalDateTime cvScoreUpdatedAt
) {

  public static AtsScoreResponse from(AtsScore score, int coinCost, LocalDateTime cvScoreUpdatedAt) {
    return new AtsScoreResponse(
        score.overallScore(),
        score.matchPercentage(),
        score.matchedSkills(),
        score.missingRequiredSkills(),
        score.missingPreferredSkills(),
        score.strengths(),
        score.weaknesses(),
        score.sections(),
        score.recommendations(),
        coinCost,
        cvScoreUpdatedAt);
  }
}
