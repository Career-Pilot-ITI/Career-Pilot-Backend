package com.careerpilot.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a clean technical/behavioral interview question sent to the client.
 * Excludes completed answer metrics (transcripts, scores, pacing) to keep response schemas elegant and easy to consume.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestionDto {
    private Long id;
    private Long sessionId;
    private String questionText;
    private Integer questionOrder;
    private LocalDateTime createdAt;
}
