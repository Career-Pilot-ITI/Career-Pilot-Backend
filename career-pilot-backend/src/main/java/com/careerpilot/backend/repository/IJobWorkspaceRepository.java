package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.JobWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IJobWorkspaceRepository extends JpaRepository<JobWorkspace, Long> {
    List<JobWorkspace> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<JobWorkspace> findByIdAndUserId(Long id, Long userId);
    Optional<JobWorkspace> findByUserIdAndJobId(Long userId, Long jobId);
    boolean existsByUserIdAndJobId(Long userId, Long jobId);
}
