package com.careerpilot.backend.dto.response;

import com.careerpilot.backend.entity.AiJob;
import com.careerpilot.backend.entity.ENUMs.AiJobStatus;
import com.careerpilot.backend.entity.ENUMs.AiJobType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

public record AiJobResponse(
    Long id,
    Long workspaceId,
    AiJobType type,
    AiJobStatus status,
    int progressPercentage,
    String currentStep,
    JsonNode result,
    String errorMessage,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime completedAt
) {

  public static AiJobResponse from(AiJob job, ObjectMapper objectMapper) {
    JsonNode result = null;
    if (job.getResult() != null) {
      try {
        result = objectMapper.readTree(job.getResult());
      } catch (Exception e) {
        result = objectMapper.getNodeFactory().textNode(job.getResult());
      }
    }
    return new AiJobResponse(
        job.getId(),
        job.getWorkspace().getId(),
        job.getType(),
        job.getStatus(),
        job.getProgressPercentage() == null ? 0 : job.getProgressPercentage(),
        job.getCurrentStep(),
        result,
        job.getErrorMessage(),
        job.getCreatedAt(),
        job.getStartedAt(),
        job.getCompletedAt()
    );
  }
}
