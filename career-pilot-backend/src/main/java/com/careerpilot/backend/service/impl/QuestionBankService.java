package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.request.CreateQuestionRequest;
import com.careerpilot.backend.dto.request.UpdateQuestionRequest;
import com.careerpilot.backend.dto.response.QuestionResponse;
import com.careerpilot.backend.entity.ENUMs.DifficultyLevel;
import com.careerpilot.backend.entity.ENUMs.QuestionCategory;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.service.IQuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class QuestionBankService implements IQuestionBankService {

    private final IQuestionBankRepository questionRepository;
    private final ITrackRepository trackRepository;


    @Override
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        log.info("Creating question for track: {}", request.getTrackId());

        // Validate track exists
        Track track = trackRepository.findById(request.getTrackId())
                .orElseThrow(() -> new RuntimeException("Track not found with ID: " + request.getTrackId()));

        // Validate difficulty level
        validateDifficultyLevel(request.getDifficultyLevel());

        // Validate category
        validateCategory(request.getCategory());

        // Create question
        QuestionBank question = new QuestionBank();
        question.setTrack(track);
        question.setQuestionText(request.getQuestionText());
        question.setDifficultyLevel(DifficultyLevel.valueOf(request.getDifficultyLevel()));
        question.setCategory(QuestionCategory.valueOf(request.getCategory()));
        question.setExpectedKeywords(request.getExpectedKeywords());
        question.setIsActive(true);
        question.setCreatedAt(LocalDateTime.now());

        QuestionBank savedQuestion = questionRepository.save(question);
        log.info("Question created with ID: {}", savedQuestion.getId());

        return mapToResponse(savedQuestion);
    }

    // ========== READ ==========

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Long id) {
        log.info("Fetching question with ID: {}", id);

        QuestionBank question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + id));

        return mapToResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getAllQuestions() {
        log.info("Fetching all questions");

        return questionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getAllQuestionsPaginated(Pageable pageable) {
        log.info("Fetching questions with pagination: page {}, size {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return questionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByTrack(Long trackId) {
        log.info("Fetching questions for track: {}", trackId);

        // Validate track exists
        trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));

        return questionRepository.findByTrackId(trackId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByTrack(Long trackId, Pageable pageable) {
        log.info("Fetching questions for track: {} with pagination", trackId);

        // Validate track exists
        trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));

        return questionRepository.findByTrackId(trackId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByDifficulty(String difficulty) {
        log.info("Fetching questions by difficulty: {}", difficulty);

        validateDifficultyLevel(difficulty);

        return questionRepository.findByDifficultyLevel(difficulty).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByDifficulty(String difficulty, Pageable pageable) {
        log.info("Fetching questions by difficulty: {} with pagination", difficulty);

        validateDifficultyLevel(difficulty);

        return questionRepository.findByDifficultyLevel(difficulty, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestionsByCategory(String category) {
        log.info("Fetching questions by category: {}", category);

        validateCategory(category);

        return questionRepository.findByCategory(category).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByCategory(String category, Pageable pageable) {
        log.info("Fetching questions by category: {} with pagination", category);

        validateCategory(category);

        return questionRepository.findByCategory(category, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> searchQuestions(String text) {
        log.info("Searching questions with text: {}", text);

        return questionRepository.findByQuestionTextContainingIgnoreCase(text).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> searchQuestions(String text, Pageable pageable) {
        log.info("Searching questions with text: {} with pagination", text);

        return questionRepository.findByQuestionTextContainingIgnoreCase(text, pageable)
                .map(this::mapToResponse);
    }

    // ========== UPDATE ==========

    @Override
    public QuestionResponse updateQuestion(Long id, UpdateQuestionRequest request) {
        log.info("Updating question with ID: {}", id);

        QuestionBank question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + id));

        // Validate difficulty level
        validateDifficultyLevel(request.getDifficultyLevel());

        // Validate category
        validateCategory(request.getCategory());

        // Update fields
        question.setQuestionText(request.getQuestionText());
        question.setDifficultyLevel(DifficultyLevel.valueOf(request.getDifficultyLevel()));
        question.setCategory(QuestionCategory.valueOf(request.getCategory()));
        question.setExpectedKeywords(request.getExpectedKeywords());

        if (request.getIsActive() != null) {
            question.setIsActive(request.getIsActive());
        }

        question.setUpdatedAt(LocalDateTime.now());

        QuestionBank updatedQuestion = questionRepository.save(question);
        log.info("Question updated with ID: {}", id);

        return mapToResponse(updatedQuestion);
    }

    @Override
    public QuestionResponse toggleQuestionStatus(Long id) {
        log.info("Toggling status for question with ID: {}", id);

        QuestionBank question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + id));

        question.setIsActive(!question.getIsActive());
        question.setUpdatedAt(LocalDateTime.now());

        QuestionBank updatedQuestion = questionRepository.save(question);
        log.info("Question status toggled for ID: {}", id);

        return mapToResponse(updatedQuestion);
    }

    // ========== DELETE ==========

    @Override
    public void deleteQuestion(Long id) {
        log.info("Deleting question with ID: {}", id);

        if (!questionRepository.existsById(id)) {
            throw new RuntimeException("Question not found with ID: " + id);
        }

        questionRepository.deleteById(id);
        log.info("Question deleted with ID: {}", id);
    }

    @Override
    public void deleteQuestionsByTrack(Long trackId) {
        log.info("Deleting all questions for track: {}", trackId);

        // Validate track exists
        trackRepository.findById(trackId)
                .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));

        List<QuestionBank> questions = questionRepository.findByTrackId(trackId);
        questionRepository.deleteAll(questions);
        log.info("Deleted {} questions for track: {}", questions.size(), trackId);
    }

    // ========== HELPER METHODS ==========

    private QuestionResponse mapToResponse(QuestionBank question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .trackId(question.getTrack().getId())
                .trackName(question.getTrack().getName())
                .questionText(question.getQuestionText())
                .difficultyLevel(String.valueOf(question.getDifficultyLevel()))
                .category(String.valueOf(question.getCategory()))
                .expectedKeywords(question.getExpectedKeywords())
                .isActive(question.getIsActive())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }

    private void validateDifficultyLevel(String difficulty) {
        if (!difficulty.matches("^(EASY|MEDIUM|HARD)$")) {
            throw new RuntimeException("Invalid difficulty level. Must be: EASY, MEDIUM, or HARD");
        }
    }

    private void validateCategory(String category) {
        if (!category.matches("^(BEHAVIORAL|TECHNICAL|PROJECT|SITUATIONAL)$")) {
            throw new RuntimeException("Invalid category. Must be: BEHAVIORAL, TECHNICAL, PROJECT, or SITUATIONAL");
        }
    }
}