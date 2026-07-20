package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubmitAnswerResponse {
    private String sessionStatus;                  // "IN_PROGRESS" or "COMPLETED"
    private QuestionScoreResponse score;           // The evaluation details for the submitted answer
    private InterviewQuestionDto nextQuestion;     // The next dynamic question, or null if complete
}
