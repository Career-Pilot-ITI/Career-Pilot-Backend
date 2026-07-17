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

}
