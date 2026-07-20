package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session metadata — used by GET /sessions and GET /sessions/{id}.
 * Does NOT embed questions. Questions live in FeedbackReportResponse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InterviewSessionResponse {
    private Long id;
    private Long trackId;
    private String trackName;
    private String status;
    private Integer overallScore;
    private Integer durationSeconds;
    private Integer targetDurationMinutes;
    private Integer maxQuestions;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
