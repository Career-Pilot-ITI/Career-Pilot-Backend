package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.Track;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ITrackRepository extends JpaRepository<Track, Long> {
    Optional<Track> findByName(String name);
    boolean existsByName(String name);
    List<Track> findByIsActiveTrue();
    long countByIsActiveTrue();

    // Relevance search: exact substring ranks first, then pg_trgm word similarity.
    // `word_similarity(a, b)` scores how close the query is to the most similar
    // word set inside the track name / description, so multi-word queries and
    // near-miss spellings still match. Returns ranked ids; load entities via
    // findById to keep the entity graph simple.
    @Query(value = """
            SELECT t.id FROM tracks t
            WHERE t.is_active = TRUE
              AND (
                t.name ILIKE '%' || :q || '%'
                OR t.description ILIKE '%' || :q || '%'
                OR word_similarity(:q, t.name) > 0.2
                OR word_similarity(:q, t.description) > 0.2
              )
            ORDER BY
                (CASE WHEN t.name ILIKE '%' || :q || '%' THEN 4 ELSE 0 END)
              + GREATEST(word_similarity(:q, t.name), word_similarity(:q, t.description))
              DESC
            """, countQuery = """
            SELECT count(*) FROM tracks t
            WHERE t.is_active = TRUE
              AND (
                t.name ILIKE '%' || :q || '%'
                OR t.description ILIKE '%' || :q || '%'
                OR word_similarity(:q, t.name) > 0.2
                OR word_similarity(:q, t.description) > 0.2
              )
            """, nativeQuery = true)
    Page<Long> searchRelevantIds(@Param("q") String q, Pageable pageable);
}