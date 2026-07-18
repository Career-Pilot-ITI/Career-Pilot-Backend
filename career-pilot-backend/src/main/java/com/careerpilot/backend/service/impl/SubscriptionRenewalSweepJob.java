package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.repository.ISubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionRenewalSweepJob {

    private final ISubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 0 * * * *")
//    @Scheduled(fixedRate = 30000)
    @Transactional
    public void sweep() {
        List<Subscription> due = subscriptionRepository
                .findByRenewalDateBeforeAndIsActiveTrue(LocalDateTime.now());

        for (Subscription sub : due) {
            if (sub.getRenewalDate() == null) {
                log.warn("Subscription {} for user {} matched sweep query but has null renewalDate — skipping",
                        sub.getId(), sub.getUser().getId());
                continue;
            }

            SubscriptionTier resolvedTier = sub.getPendingTier() != null
                    ? sub.getPendingTier()
                    : SubscriptionTier.FREE;

            log.info("Subscription {} for user {} renewing: {} -> {}",
                    sub.getId(), sub.getUser().getId(), sub.getTier(), resolvedTier);

            sub.setTier(resolvedTier);
            sub.setPendingTier(null);
            sub.setCancelledAt(null);
            sub.setRenewalDate(resolvedTier == SubscriptionTier.FREE ? null : LocalDateTime.now().plusMonths(1));
            sub.setUpdatedAt(LocalDateTime.now());
        }

        subscriptionRepository.saveAll(due);
    }
}