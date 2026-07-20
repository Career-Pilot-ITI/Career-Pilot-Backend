package com.careerpilot.backend.service;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.annotation.RedactPii;
import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.ScoreResponse;

import java.util.List;

public interface ILlmService {
  @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
  @RedactPii
  List<GeneratedQuestion> generateQuestions(Long trackId, int count);

  @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
  @RedactPii
  ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript);

  @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
  @RedactPii
  List<String> generateSessionTips(Long sessionId, Long userId);

  @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
  @RedactPii
  String generateQuestionTip(String questionText, String transcript,
      int contentRelevance, int clarity, int confidence, int pacing, int fillerWords);

  @RateLimit(capacity = 2, refillTokens = 2, refillSeconds = 60)
  @RedactPii
  CvAnalysis analyzeCv(String cvText);
}
