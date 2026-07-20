package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response returned by POST /api/v1/interviews/sessions (start session).
 *
 * The frontend only needs three things to run the interview:
 *  - sessionId          → used in every subsequent request
 *  - targetDurationMinutes → how long to run the client-side timer for
 *  - currentQuestion    → the first question to display immediately
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StartSessionResponse {
    private Long sessionId;
    private String trackName;
    private Integer targetDurationMinutes;
    private Integer maxQuestions;
    private LocalDateTime startedAt;
    private InterviewQuestionDto currentQuestion;  // question #1, ready to display
}
