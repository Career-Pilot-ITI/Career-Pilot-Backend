package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.JobListing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IJobListingRepository extends JpaRepository<JobListing, Long> {
    List<JobListing> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<JobListing> findBySourceUrl(String sourceUrl);
}
