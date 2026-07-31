package com.careerpilot.backend.service;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.annotation.RedactPii;
import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.JobDraft;
import com.careerpilot.backend.dto.response.ScoreResponse;

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
}
