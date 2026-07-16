package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.QuestionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IQuestionScoreRepository extends JpaRepository<QuestionScore, Long> {

    Optional<QuestionScore> findBySessionQuestionId(Long sessionQuestionId);

    boolean existsBySessionQuestionId(Long sessionQuestionId);
}
