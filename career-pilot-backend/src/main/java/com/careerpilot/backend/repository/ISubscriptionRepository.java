package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ISubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
    List<Subscription> findByRenewalDateBeforeAndIsActiveTrue(LocalDateTime cutoff);
}