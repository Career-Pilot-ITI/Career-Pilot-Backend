package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.AiJob;
import com.careerpilot.backend.entity.ENUMs.AiJobStatus;
import com.careerpilot.backend.entity.ENUMs.AiJobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IAiJobRepository extends JpaRepository<AiJob, Long> {

    Optional<AiJob> findByIdAndUserId(Long id, Long userId);

    @Query("SELECT j FROM AiJob j WHERE j.workspace.id = :workspaceId AND j.type = :type AND j.status IN (:statuses) ORDER BY j.createdAt DESC")
    List<AiJob> findActiveByWorkspaceAndType(
            @Param("workspaceId") Long workspaceId,
            @Param("type") AiJobType type,
            @Param("statuses") List<AiJobStatus> statuses
    );
}
