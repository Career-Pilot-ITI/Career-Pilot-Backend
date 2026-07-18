package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.Subscription;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubscriptionResponse {
    private String tier;
    private Boolean isActive;
    private LocalDateTime startedAt;
    private LocalDateTime renewalDate;
    private LocalDateTime cancelledAt;
    private String pendingTier; // null if no change scheduled

    public static SubscriptionResponse from(Subscription sub) {
        SubscriptionResponse r = new SubscriptionResponse();
        r.setTier(sub.getTier() != null ? sub.getTier().toString() : null);
        r.setIsActive(sub.getIsActive());
        r.setStartedAt(sub.getStartedAt());
        r.setRenewalDate(sub.getRenewalDate());
        r.setCancelledAt(sub.getCancelledAt());
        r.setPendingTier(sub.getPendingTier() != null ? sub.getPendingTier().toString() : null);
        return r;
    }
}