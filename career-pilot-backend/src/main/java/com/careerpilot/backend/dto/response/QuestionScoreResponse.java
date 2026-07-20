package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionScoreResponse {
  private Long id;
  private Long sessionQuestionId;
  private Integer contentRelevance; // 40% weight
  private Integer clarity; // 20% weight
  private Integer confidence; // 20% weight
  private Integer pacing; // 10% weight
  private Integer fillerWords; // 10% weight
  private Integer overallScore; // weighted average
  private String coachingTip;
  private LocalDateTime createdAt;
}
