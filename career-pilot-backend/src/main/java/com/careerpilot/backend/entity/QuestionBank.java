package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.DifficultyLevel;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "question_bank")
public class QuestionBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "difficulty_level")
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel;  // EASY, MEDIUM, HARD

    @Column(name = "category")
    private String category;          // "behavioral", "technical", "project"

    @Column(name = "expected_keywords")
    private String expectedKeywords;  // JSON array

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Relationships
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    private List<UserQuestionHistory> history;
}

