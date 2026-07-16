package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.FeedbackReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IFeedbackReportRepository extends JpaRepository<FeedbackReport, Long> {
    Optional<FeedbackReport> findBySessionId(Long sessionId);
    List<FeedbackReport> findBySessionUserIdOrderByCreatedAtDesc(Long userId);
}
