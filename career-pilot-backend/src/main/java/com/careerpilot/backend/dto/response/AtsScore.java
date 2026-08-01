package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AtsScore(
    int overallScore,
    int matchPercentage,
    List<String> matchedSkills,
    List<String> missingRequiredSkills,
    List<String> missingPreferredSkills,
    List<String> strengths,
    List<String> weaknesses,
    List<SectionScore> sections,
    List<String> recommendations
) {

  public AtsScore {
    matchedSkills = matchedSkills != null ? matchedSkills : List.of();
    missingRequiredSkills = missingRequiredSkills != null ? missingRequiredSkills : List.of();
    missingPreferredSkills = missingPreferredSkills != null ? missingPreferredSkills : List.of();
    strengths = strengths != null ? strengths : List.of();
    weaknesses = weaknesses != null ? weaknesses : List.of();
    sections = sections != null ? sections : List.of();
    recommendations = recommendations != null ? recommendations : List.of();
  }

  public AtsScore() {
    this(0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
  }
}
