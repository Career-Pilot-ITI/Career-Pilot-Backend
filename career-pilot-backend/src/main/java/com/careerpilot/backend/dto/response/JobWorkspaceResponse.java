package com.careerpilot.backend.dto.response;

import com.careerpilot.backend.entity.JobWorkspace;
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
public class JobWorkspaceResponse {
  private Long id;
  private JobListingResponse job;
  private String status;
  private Integer cvScore;
  private LocalDateTime cvScoreUpdatedAt;
  private String cvOptimizedText;
  private String coverLetterText;
  private Long lastInterviewSessionId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static JobWorkspaceResponse from(JobWorkspace ws) {
    if (ws == null)
      return null;
    return JobWorkspaceResponse.builder()
        .id(ws.getId())
        .job(JobListingResponse.from(ws.getJob()))
        .status(ws.getStatus() != null ? ws.getStatus().name() : null)
        .cvScore(ws.getCvScore())
        .cvScoreUpdatedAt(ws.getCvScoreUpdatedAt())
        .cvOptimizedText(ws.getCvOptimizedText())
        .coverLetterText(ws.getCoverLetterText())
        .lastInterviewSessionId(ws.getLastInterviewSession() != null
            ? ws.getLastInterviewSession().getId()
            : null)
        .createdAt(ws.getCreatedAt())
        .updatedAt(ws.getUpdatedAt())
        .build();
  }
}
