package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeedbackReportResponse {

    private Long id;
    private Long sessionId;

    private Integer overallScore;
    private Integer clarityScore;
    private Integer confidenceScore;
    private Integer pacingScore;
    private Integer fillerWordsScore;
    private Integer contentRelevanceScore;

    private List<String> coachingTips;

    private LocalDateTime generatedAt;
    private LocalDateTime createdAt;

    private List<SessionQuestionResponse> questions;
}
