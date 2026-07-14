package com.careerpilot.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_reports")
public class FeedbackReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private InterviewSession session;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "clarity_score")
    private Integer clarityScore;

    @Column(name = "confidence_score")
    private Integer confidenceScore;

    @Column(name = "pacing_score")
    private Integer pacingScore;

    @Column(name = "filler_words_score")
    private Integer fillerWordsScore;

    @Column(name = "content_relevance_score")
    private Integer contentRelevanceScore;

    @Column(name = "coaching_tips", columnDefinition = "TEXT")
    private String coachingTips;      // JSON array أو formatted text

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}