package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.InterviewSession;
import com.careerpilot.backend.entity.ENUMs.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IInterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);

    List<InterviewSession> findByUserIdAndStatus(Long userId, SessionStatus status);
}
