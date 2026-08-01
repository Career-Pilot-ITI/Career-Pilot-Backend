package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CoverLetterDraft(
    String coverLetter,
    String approachTips
) {

  public CoverLetterDraft() {
    this("", "");
  }
}
