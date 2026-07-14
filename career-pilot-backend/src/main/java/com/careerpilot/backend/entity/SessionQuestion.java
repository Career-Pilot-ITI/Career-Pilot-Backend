package com.careerpilot.backend.entity;
import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(name = "session_questions")
public class SessionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionBank question;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;    // 1, 2, 3...

    @Column(name = "user_transcript", columnDefinition = "TEXT")
    private String userTranscript;    // ما اللي قال المستخدم

    @Column(name = "audio_url")
    private String audioUrl;          // S3 URL للصوت

    @Column(name = "generated_by_llm")
    private Boolean generatedByLlm = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Relationship
    @OneToOne(mappedBy = "sessionQuestion", cascade = CascadeType.ALL)
    private QuestionScore score;
}