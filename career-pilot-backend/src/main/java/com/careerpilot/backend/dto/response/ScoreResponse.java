package com.careerpilot.backend.dto.response;

public record ScoreResponse(
    int contentRelevance,
    int clarity,
    int confidence,
    int fillerWords,
    String reasoning
) {}
