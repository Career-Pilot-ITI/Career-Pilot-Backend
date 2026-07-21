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
public class SessionStateResponse {
    private Long sessionId;
    private String status;
    private String trackName;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private int answeredCount;
    private int totalCount;
    private List<SessionQuestionResponse> answeredQuestions;
    private InterviewQuestionDto currentQuestion;
}
