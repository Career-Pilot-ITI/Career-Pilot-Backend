package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.SubscriptionException;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.ISubscriptionRepository;
import com.careerpilot.backend.service.ISubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements ISubscriptionService {

    private final ISubscriptionRepository subscriptionRepository;

    @Override
    @Transactional
    public Subscription assignFreeTierOnRegistration(User user) {
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setTier(SubscriptionTier.FREE);
        sub.setIsActive(true);
        sub.setStartedAt(LocalDateTime.now());
        sub.setCreatedAt(LocalDateTime.now());
        return subscriptionRepository.save(sub);
    }

    @Override
    public SubscriptionTier getCurrentTier(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(Subscription::getTier)
                .orElse(SubscriptionTier.FREE); // default per decision — resilient for pre-fix users
    }

    @Override
    public Subscription getCurrentSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new SubscriptionException.NoActiveSubscriptionException(
                        "No subscription found for user: " + userId));
    }

    @Override
    @Transactional
    public void upgrade(User user, SubscriptionTier newTier) {
        Subscription sub = subscriptionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Subscription s = new Subscription();
                    s.setUser(user);
                    s.setStartedAt(LocalDateTime.now());
                    s.setCreatedAt(LocalDateTime.now());
                    return s;
                });
        sub.setTier(newTier);
        sub.setIsActive(true);
        sub.setCancelledAt(null);
        sub.setPendingTier(null); // clear any previously-scheduled downgrade
        sub.setRenewalDate(LocalDateTime.now().plusMonths(1));
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional
    public void downgrade(Long userId, SubscriptionTier newTier) {
        Subscription sub = getCurrentSubscription(userId);
        if (sub.getRenewalDate() == null) {
            throw new SubscriptionException.InvalidTierException(
                    "Cannot schedule downgrade: no active billing cycle");
        }
        if (newTier.ordinal() >= sub.getTier().ordinal()) {
            throw new SubscriptionException.InvalidTierException(
                    "Downgrade target must be lower than current tier");
        }
        sub.setPendingTier(newTier);
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }

    @Override
    @Transactional
    public void cancel(Long userId) {
        Subscription sub = getCurrentSubscription(userId);
        sub.setPendingTier(SubscriptionTier.FREE);
        sub.setCancelledAt(LocalDateTime.now());
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }
}