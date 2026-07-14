package com.careerpilot.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Setter
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "target_role")
    private String targetRole;

    @Column(name = "industry")
    private String industry;

    @Column(name = "experience_level", length = 20)
    private String experienceLevel;

    @Column(name = "current_job_title")
    private String currentJobTitle;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Column(name = "cv_filename")
    private String cvFilename;

    @Column(name = "cv_uploaded_at")
    private LocalDateTime cvUploadedAt;

    @Column(name = "target_companies", columnDefinition = "TEXT")
    private String targetCompanies;

    @Column(name = "education_level", length = 20)
    private String educationLevel;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Column(name = "terms_accepted")
    private Boolean termsAccepted = false;

    @Column(name = "subscription_tier")
    private String subscriptionTier;

    @Column(name = "coin_balance", nullable = false)
    private Integer coinBalance = 0;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
