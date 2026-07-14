package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "tier", nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionTier tier;    // FREE, PLUS, PRO

    @Column(name = "renewal_date")
    private LocalDateTime renewalDate;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}


