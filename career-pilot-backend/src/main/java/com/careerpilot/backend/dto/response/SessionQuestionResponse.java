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
public class SessionQuestionResponse {
    private Long id;
    private Long sessionId;
    private String questionText;
    private Integer questionOrder;
    private String userTranscript;
    private Long durationMs;

    // Pacing metrics (computed server-side)
    private Double speechRateWpm;
    private Double avgPauseMs;
    private Double silenceRatio;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Score details (present after scoring completes)
    private QuestionScoreResponse score;
}
