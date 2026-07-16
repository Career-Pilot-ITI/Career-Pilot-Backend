package com.careerpilot.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "session_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    private String userTranscript;    // The STT text from mobile

    @Column(name = "audio_url")
    private String audioUrl;          // null — audio not stored

    @Column(name = "duration_ms")
    private Long durationMs;          // total answer duration in milliseconds

    @Column(name = "word_timings_json", columnDefinition = "TEXT")
    private String wordTimingsJson;   // JSON array of WordTiming objects

    // Pacing metrics computed server-side
    @Column(name = "speech_rate_wpm")
    private Double speechRateWpm;     // words per minute

    @Column(name = "avg_pause_ms")
    private Double avgPauseMs;        // average pause between words (ms)

    @Column(name = "silence_ratio")
    private Double silenceRatio;      // total pause time / total duration

    @Column(name = "generated_by_llm")
    private Boolean generatedByLlm = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Relationship
    @OneToOne(mappedBy = "sessionQuestion", cascade = CascadeType.ALL)
    private QuestionScore score;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}