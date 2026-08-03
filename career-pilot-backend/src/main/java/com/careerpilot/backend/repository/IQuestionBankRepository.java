package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.QuestionBank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT q FROM QuestionBank q JOIN FETCH q.track WHERE q.isActive = TRUE")
    List<QuestionBank> findAllActiveWithTrack();

    @Query("SELECT COUNT(q) FROM QuestionBank q WHERE q.isActive = TRUE")
    long countActiveWithTrack();

    List<QuestionBank> findByTrackIdAndDifficultyLevel(Long trackId, String difficultyLevel);
    Page<QuestionBank> findByTrackIdAndDifficultyLevel(Long trackId, String difficultyLevel, Pageable pageable);

    // Search by text
    List<QuestionBank> findByQuestionTextContainingIgnoreCase(String text);
    Page<QuestionBank> findByQuestionTextContainingIgnoreCase(String text, Pageable pageable);

    // Relevance search: exact substring ranks first, then pg_trgm word similarity.
    // `word_similarity(a, b)` scores how close the query is to the most similar
    // word set inside the question text / keywords, so multi-word queries and
    // near-miss spellings still match. Returns ranked ids; load entities via
    // findActiveByIdsIn to keep the lazy track association populated.
    @Query(value = """
            SELECT qb.id FROM question_bank qb
            WHERE qb.is_active = TRUE
              AND (
                qb.question_text ILIKE '%' || :q || '%'
                OR qb.expected_keywords ILIKE '%' || :q || '%'
                OR word_similarity(:q, qb.question_text) > 0.2
                OR word_similarity(:q, qb.expected_keywords) > 0.2
              )
            ORDER BY
                (CASE WHEN qb.question_text ILIKE '%' || :q || '%' THEN 4 ELSE 0 END)
              + (CASE WHEN qb.expected_keywords ILIKE '%' || :q || '%' THEN 2 ELSE 0 END)
              + GREATEST(word_similarity(:q, qb.question_text), word_similarity(:q, qb.expected_keywords))
              DESC
            """, countQuery = """
            SELECT count(*) FROM question_bank qb
            WHERE qb.is_active = TRUE
              AND (
                qb.question_text ILIKE '%' || :q || '%'
                OR qb.expected_keywords ILIKE '%' || :q || '%'
                OR word_similarity(:q, qb.question_text) > 0.2
                OR word_similarity(:q, qb.expected_keywords) > 0.2
              )
            """, nativeQuery = true)
    Page<Long> searchRelevantIds(@Param("q") String q, Pageable pageable);

    @Query("SELECT q FROM QuestionBank q JOIN FETCH q.track WHERE q.isActive = TRUE AND q.id IN :ids")
    List<QuestionBank> findActiveByIdsIn(@Param("ids") java.util.Collection<Long> ids);

    // Used by InterviewSessionService to pick active questions when starting a session
    List<QuestionBank> findByTrackIdAndIsActiveTrue(Long trackId);
    Page<QuestionBank> findByTrackIdAndIsActiveTrue(Long trackId, Pageable pageable);
}
