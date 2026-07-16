package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.SessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ISessionQuestionRepository extends JpaRepository<SessionQuestion, Long> {

    List<SessionQuestion> findBySessionIdOrderByQuestionOrderAsc(Long sessionId);

    Optional<SessionQuestion> findByIdAndSessionId(Long id, Long sessionId);

    @Query("SELECT COUNT(sq) FROM SessionQuestion sq WHERE sq.session.id = :sessionId")
    int countBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT COUNT(sq) FROM SessionQuestion sq WHERE sq.session.id = :sessionId AND sq.completedAt IS NOT NULL")
    int countAnsweredBySessionId(@Param("sessionId") Long sessionId);

    @Query("SELECT COALESCE(MAX(sq.questionOrder), 0) FROM SessionQuestion sq WHERE sq.session.id = :sessionId")
    int findMaxOrderBySessionId(@Param("sessionId") Long sessionId);
}
