package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.CreateQuestionRequest;
import com.careerpilot.backend.dto.request.UpdateQuestionRequest;
import com.careerpilot.backend.dto.response.QuestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IQuestionBankService {

    // Create
    QuestionResponse createQuestion(CreateQuestionRequest request);

    // Read
    QuestionResponse getQuestion(Long id);
    List<QuestionResponse> getAllQuestions();
    Page<QuestionResponse> getAllQuestionsPaginated(Pageable pageable);

    // Read by filters
    List<QuestionResponse> getQuestionsByTrack(Long trackId);
    Page<QuestionResponse> getQuestionsByTrack(Long trackId, Pageable pageable);

    List<QuestionResponse> getQuestionsByDifficulty(String difficulty);
    Page<QuestionResponse> getQuestionsByDifficulty(String difficulty, Pageable pageable);

    List<QuestionResponse> getQuestionsByCategory(String category);
    Page<QuestionResponse> getQuestionsByCategory(String category, Pageable pageable);

    List<QuestionResponse> searchQuestions(String text);
    Page<QuestionResponse> searchQuestions(String text, Pageable pageable);

    // Update
    QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request);
    QuestionResponse toggleQuestionStatus(Long id);

    // Delete
    void deleteQuestion(Long id);
    void deleteQuestionsByTrack(Long trackId);
}