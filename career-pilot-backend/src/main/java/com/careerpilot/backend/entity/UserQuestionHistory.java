package com.careerpilot.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "user_question_history")
public class UserQuestionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionBank question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @Column(name = "score_received")
    private Integer scoreReceived;    // 0-100

    @Column(name = "asked_at", nullable = false)
    private LocalDateTime askedAt;

    @Column(name = "how_many_times_asked", nullable = false)
    private Integer howManyTimesAsked = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Computed property
    public Integer getDaysSinceLastAsked() {
        return Math.toIntExact(
                ChronoUnit.DAYS.between(askedAt, LocalDateTime.now())
        );
    }
}