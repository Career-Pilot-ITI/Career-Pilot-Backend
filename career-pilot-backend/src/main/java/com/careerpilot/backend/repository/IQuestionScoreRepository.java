package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.QuestionScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IQuestionScoreRepository extends JpaRepository<QuestionScore, Long> {

  Optional<QuestionScore> findBySessionQuestionId(Long sessionQuestionId);

  @Query("SELECT qs FROM QuestionScore qs WHERE qs.sessionQuestion.id IN :ids")
  List<QuestionScore> findBySessionQuestionIdIn(@Param("ids") List<Long> sessionQuestionIds);

  boolean existsBySessionQuestionId(Long sessionQuestionId);
}
