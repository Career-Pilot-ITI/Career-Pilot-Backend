package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.JobSourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // many job to single user (who creates the job but also the job is shared with other users)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "seniority_level", length = 50)
    private String seniorityLevel;

    //used json for storing required and preferred skills
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", nullable = false)
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_skills", nullable = false)
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @Column(name = "responsibilities")
    private String responsibilities;

    @Column(name = "qualifications")
    private String qualifications;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "technologies", nullable = false)
    @Builder.Default
    private List<String> technologies = new ArrayList<>();

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    // Cache/dedup key 
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobSourceType sourceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
