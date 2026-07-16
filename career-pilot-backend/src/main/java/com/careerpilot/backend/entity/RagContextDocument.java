package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.DocType;
import com.pgvector.PGvector;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "rag_context_documents")
@Data
public class RagContextDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "doc_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocType docType;          // SESSION_SUMMARY, CV_EXTRACT, PERF_TREND, USER_PROFILE

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;           // النص اللي ال LLM هيقرأه

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "vector", columnDefinition = "vector(1536)")
    private PGvector vector;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }
}

