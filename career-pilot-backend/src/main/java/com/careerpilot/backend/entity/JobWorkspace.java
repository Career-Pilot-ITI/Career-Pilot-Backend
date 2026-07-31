package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.JobWorkspaceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_workspaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobWorkspace {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // this is a unique constraint in the space of users which means each job
  // workspace is unique for a user
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_id", nullable = false)
  private JobListing job;

  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @Builder.Default
  private JobWorkspaceStatus status = JobWorkspaceStatus.SAVED;

  @Column(name = "cv_score")
  private Integer cvScore;

  @Column(name = "cv_score_updated_at")
  private LocalDateTime cvScoreUpdatedAt;

  @Column(name = "cv_optimized_text")
  private String cvOptimizedText;

  @Column(name = "cover_letter_text")
  private String coverLetterText;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_interview_session_id")
  private InterviewSession lastInterviewSession;

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
