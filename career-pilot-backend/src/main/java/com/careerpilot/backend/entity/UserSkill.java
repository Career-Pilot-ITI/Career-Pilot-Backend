package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.SkillCategory;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_skills")
public class UserSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    private SkillCategory category;

    @Column(name = "performance_score")
    private Integer performanceScore = 0;

    @Column(name = "times_assessed", nullable = false)
    private Integer timesAssessed = 0;

    @Column(name = "last_assessed_at")
    private LocalDateTime lastAssessedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

