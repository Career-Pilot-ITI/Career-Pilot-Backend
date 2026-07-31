package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobDraft(
    String title,
    String companyName,
    String location,
    String description,
    String employmentType,
    String seniorityLevel,
    List<String> requiredSkills,
    List<String> preferredSkills,
    List<String> technologies,
    Integer salaryMin,
    Integer salaryMax,
    String currency,
    Integer experienceYears,
    String educationLevel) {

  public JobDraft {
    requiredSkills = requiredSkills != null ? requiredSkills : List.of();
    preferredSkills = preferredSkills != null ? preferredSkills : List.of();
    technologies = technologies != null ? technologies : List.of();
  }
}
