package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface IInterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

    List<InterviewSession> findByUserIdAndStatus(Long userId, SessionStatus status);

    @Query("SELECT COUNT(s) FROM InterviewSession s WHERE s.user.id = :userId AND s.createdAt >= :since")
    long countByUserIdAndCreatedAtAfter(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    long countByUserId(Long userId);
}
