package com.careerpilot.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;              // "Senior Backend Engineer"

    @Column(name = "cv_url")
    private String cvUrl;             // S3 URL or file path

    @Column(name = "cv_filename")
    private String cvFilename;

    @Column(name = "cv_uploaded_at")
    private LocalDateTime cvUploadedAt;

    @Column(name = "subscription_tier")
    private String subscriptionTier;  // "free", "plus", "pro"

    @Column(name = "coin_balance", nullable = false)
    private Integer coinBalance = 0;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}