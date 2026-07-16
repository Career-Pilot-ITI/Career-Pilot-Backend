package com.careerpilot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "question_scores")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_question_id", nullable = false, unique = true)
    private SessionQuestion sessionQuestion;

    @Column(name = "clarity", nullable = false)
    private Integer clarity;          // 0-100

    @Column(name = "confidence", nullable = false)
    private Integer confidence;       // 0-100

    @Column(name = "pacing", nullable = false)
    private Integer pacing;           // 0-100

    @Column(name = "filler_words", nullable = false)
    private Integer fillerWords;      // 0-100

    @Column(name = "content_relevance", nullable = false)
    private Integer contentRelevance; // 0-100

    @Column(name = "overall_question_score", nullable = false)
    private Integer overallScore;     // weighted average

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void calculateOverallScore() {
        this.overallScore = (contentRelevance * 40 +
                clarity * 20 +
                confidence * 20 +
                pacing * 10 +
                fillerWords * 10) / 100;
    }
}
