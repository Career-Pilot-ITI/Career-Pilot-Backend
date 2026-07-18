package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.entity.User;

public interface ISubscriptionService {
    Subscription assignFreeTierOnRegistration(User user);
    SubscriptionTier getCurrentTier(Long userId);
    Subscription getCurrentSubscription(Long userId);
    void upgrade(User user, SubscriptionTier newTier);
    void downgrade(Long userId, SubscriptionTier newTier);
    void cancel(Long userId);
}