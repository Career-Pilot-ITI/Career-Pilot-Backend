package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IQuestionBankRepository extends JpaRepository<QuestionBank, Long> {

    List<QuestionBank> findByTrackId(Long trackId);
    Page<QuestionBank> findByTrackId(Long trackId, Pageable pageable);

    List<QuestionBank> findByDifficultyLevel(String difficultyLevel);
    Page<QuestionBank> findByDifficultyLevel(String difficultyLevel, Pageable pageable);

    List<QuestionBank> findByCategory(String category);
    Page<QuestionBank> findByCategory(String category, Pageable pageable);

    List<QuestionBank> findByIsActiveTrue();
    Page<QuestionBank> findByIsActiveTrue(Pageable pageable);

    List<QuestionBank> findByTrackIdAndDifficultyLevel(Long trackId, String difficultyLevel);
    Page<QuestionBank> findByTrackIdAndDifficultyLevel(Long trackId, String difficultyLevel, Pageable pageable);

    // Search by text
    List<QuestionBank> findByQuestionTextContainingIgnoreCase(String text);
    Page<QuestionBank> findByQuestionTextContainingIgnoreCase(String text, Pageable pageable);

    // Used by InterviewSessionService to pick active questions when starting a session
    List<QuestionBank> findByTrackIdAndIsActiveTrue(Long trackId);
}