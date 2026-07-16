package com.careerpilot.backend.service;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.ScoreResponse;

import java.util.List;

public interface ILlmService {
    @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
    List<GeneratedQuestion> generateQuestions(Long trackId, int count);

    @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
    ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript);

    @RateLimit(capacity = 10, refillTokens = 10, refillSeconds = 60)
    List<String> generateTips(String transcript, ScoreResponse scores);

    @RateLimit(capacity = 2, refillTokens = 2, refillSeconds = 60)
    CvAnalysis analyzeCv(String cvText);
}
