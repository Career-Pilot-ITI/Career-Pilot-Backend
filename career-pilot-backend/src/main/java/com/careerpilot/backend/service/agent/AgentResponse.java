package com.careerpilot.backend.service.agent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentResponse {
    private int contentRelevance;
    private int clarity;
    private int confidence;
    private int fillerWords;
    private String reasoning;
    private String nextQuestion;
    private Long sourceQuestionId;
    private String coachingTip;
    private String sessionStatus;

    public static AgentResponse fallback(String questionText) {
        return AgentResponse.builder()
                .contentRelevance(0).clarity(0).confidence(0).fillerWords(0)
                .reasoning("Agent processing failed. Using fallback.")
                .nextQuestion("Can you elaborate on your experience with this topic?")
                .sourceQuestionId(null)
                .coachingTip("Keep practicing.")
                .sessionStatus("IN_PROGRESS")
                .build();
    }
}
