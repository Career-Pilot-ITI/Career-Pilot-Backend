package com.careerpilot.backend.dto.response;

public record CoverLetterResponse(
    String coverLetter,
    String approachTips,
    int coinCost
) {

  public static CoverLetterResponse from(CoverLetterDraft draft, int coinCost) {
    return new CoverLetterResponse(
        draft.coverLetter(),
        draft.approachTips(),
        coinCost);
  }
}
