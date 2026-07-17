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
public class InterviewSessionResponse {

    private Long id;
    private Long trackId;
    private String trackName;
    private String status;          // IN_PROGRESS, COMPLETED, ABANDONED
    private Integer overallScore;   // null until feedback is generated
    private Integer durationSeconds;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Questions attached to this session (ordered by questionOrder)
    private List<SessionQuestionResponse> questions;
}
