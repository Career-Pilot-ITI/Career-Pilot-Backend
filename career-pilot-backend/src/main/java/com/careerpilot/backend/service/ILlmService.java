package com.careerpilot.backend.service;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.annotation.RedactPii;
import com.careerpilot.backend.dto.response.AtsScore;
import com.careerpilot.backend.dto.response.CoverLetterDraft;
import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.CvSection;
import com.careerpilot.backend.dto.response.CvSectionDto;
import com.careerpilot.backend.dto.response.JobDraft;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserSkill;

import java.util.List;

public interface ILlmService {
  @RateLimit(capacity = 10, refillTokens = 10)
  @RedactPii
  ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript);

  @RateLimit
  @RedactPii
  List<String> generateSessionTips(Long sessionId, Long userId);

  @RateLimit(capacity = 10, refillTokens = 10)
  @RedactPii
  String generateQuestionTip(String questionText, String transcript,
      int contentRelevance, int clarity, int confidence, int pacing, int fillerWords);

  @RateLimit(capacity = 2, refillTokens = 2)
  @RedactPii
  CvAnalysis analyzeCv(String cvText);

  @RateLimit
  @RedactPii
  JobDraft parseJobPosting(String rawText);

  @RateLimit
  @RedactPii
  AtsScore scoreCv(String cvText, JobListing job);

  @RateLimit(key = "#userId", capacity = 10, refillTokens = 10, refillSeconds = 60)
  List<CvSectionDto> splitCvIntoSections(Long userId, String cvText);

  @RateLimit(key = "#userId", capacity = 30, refillTokens = 30, refillSeconds = 60)
  CvSection optimizeSection(Long userId, String sectionName, String sectionContent, JobListing job, List<UserSkill> skills);

  @RateLimit(key = "#userId", capacity = 10, refillTokens = 10, refillSeconds = 60)
  List<String> recommendTracks(Long userId, String cvText, List<Track> tracks);

  @RateLimit
  @RedactPii
  CoverLetterDraft generateCoverLetter(String cvText, JobListing job, UserProfile profile,
      String companyResearch, SubscriptionTier tier);
}
