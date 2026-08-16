package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.JobWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IJobWorkspaceRepository extends JpaRepository<JobWorkspace, Long> {
    List<JobWorkspace> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT jw FROM JobWorkspace jw JOIN FETCH jw.job WHERE jw.id = ?1 AND jw.user.id = ?2")
    Optional<JobWorkspace> findByIdAndUserId(Long id, Long userId);
    Optional<JobWorkspace> findByUserIdAndJobId(Long userId, Long jobId);
    Optional<JobWorkspace> findByLastInterviewSessionId(Long sessionId);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
}
