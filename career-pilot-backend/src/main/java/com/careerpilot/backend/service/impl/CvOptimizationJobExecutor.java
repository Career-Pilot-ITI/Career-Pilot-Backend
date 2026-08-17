package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.CvOptimizationResponse;
import com.careerpilot.backend.dto.response.CvSection;
import com.careerpilot.backend.dto.response.CvSectionDto;
import com.careerpilot.backend.entity.AiJob;
import com.careerpilot.backend.entity.ENUMs.AiJobStatus;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.repository.IAiJobRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.repository.IUserSkillRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CvOptimizationJobExecutor {

    private final IAiJobRepository aiJobRepository;
    private final ILlmService llmService;
    private final IUserSkillRepository userSkillRepository;
    private final ITrackRepository trackRepository;
    private final ICoinWalletService coinWalletService;
    private final ObjectMapper objectMapper;
    private final TaskExecutor taskExecutor;

    // Free-tier LLM endpoints rate-limit bursts of concurrent calls. Bounding
    // the section fan-out avoids a wall of simultaneous requests timing out and
    // silently producing empty score-0 sections (the observed production bug).
    private java.util.concurrent.Semaphore sectionPermits;

    @org.springframework.beans.factory.annotation.Value("${app.cv-optimize.max-parallel-sections:2}")
    private int maxParallelSections;

    @jakarta.annotation.PostConstruct
    void initSectionPermits() {
        sectionPermits = new java.util.concurrent.Semaphore(maxParallelSections);
    }

    @Async
    public void executeOptimization(Long jobId, Long userId, String rawCvText, JobListing job, int coinCost) {
        log.info("Starting CV optimization job ID: {} for user: {}", jobId, userId);

        AiJob aiJob = aiJobRepository.findById(jobId).orElse(null);
        if (aiJob == null) {
            log.error("AI Job not found: {}", jobId);
            return;
        }

        try {
            // Update job status to PROCESSING
            aiJob.setStatus(AiJobStatus.PROCESSING);
            aiJob.setStartedAt(LocalDateTime.now());
            aiJob.setProgressPercentage(10);
            aiJob.setCurrentStep("Preparing CV and redacting sensitive data...");
            aiJobRepository.save(aiJob);

            // Manual reversible redaction: builds one index over the whole
            // normalized CV, then restores it against the final assembled JSON.
            // @RedactPii can't replace this - it only redacts/restores the
            // String args of a single proxied call, but our result is produced
            // by N LLM calls (split -> optimize each section -> tracks).
            String normalized = rawCvText.replaceAll("\\s+", " ").trim();
            PiiRedactionUtil.RedactionResult piiResult = PiiRedactionUtil.redactWithIndex(normalized);
            String redactedCv = piiResult.redactedContent();

            // Step 2: Split CV into primary logical sections
            aiJob.setCurrentStep("Analyzing CV structure...");
            aiJob.setProgressPercentage(20);
            aiJobRepository.save(aiJob);

            List<CvSectionDto> parsedSections = llmService.splitCvIntoSections(userId, redactedCv);
            if (parsedSections.isEmpty()) {
                parsedSections = List.of(new CvSectionDto("Full Profile", redactedCv));
            }

            // Step 3: Optimize every section in parallel so wall-clock time is
            // bounded by the slowest section, not the sum of all of them.
            List<UserSkill> skills = userSkillRepository.findByUserId(userId);

            aiJob.setCurrentStep("Optimizing CV sections in parallel...");
            aiJob.setProgressPercentage(25);
            aiJobRepository.save(aiJob);

            List<CvSection> optimizedSections = optimizeSectionsInParallel(userId, parsedSections, job, skills);

            // Step 4: Recommend matching tracks
            aiJob.setCurrentStep("Generating matching career tracks...");
            aiJob.setProgressPercentage(85);
            aiJobRepository.save(aiJob);

            List<Track> tracks = trackRepository.findByIsActiveTrue();
            List<String> recommendedTracks = llmService.recommendTracks(userId, redactedCv, tracks);

            // Step 5: Assemble and restore PII in final DTO
            CvOptimizationResponse responseDto = new CvOptimizationResponse(optimizedSections, recommendedTracks, coinCost);
            String rawJsonResponse = objectMapper.writeValueAsString(responseDto);

            // Restore redacted content
            String finalJsonResponse = piiResult.restore(rawJsonResponse);

            // Update database
            aiJob.setResult(finalJsonResponse);
            aiJob.setStatus(AiJobStatus.COMPLETED);
            aiJob.setProgressPercentage(100);
            aiJob.setCurrentStep("CV Optimization completed successfully!");
            aiJob.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(aiJob);

            log.info("CV optimization job ID: {} completed successfully", jobId);

        } catch (Exception e) {
            log.error("Failed executing CV optimization job: {}", jobId, e);

            // Fail job
            aiJob.setStatus(AiJobStatus.FAILED);
            aiJob.setErrorMessage(e.getMessage());
            aiJob.setCompletedAt(LocalDateTime.now());
            aiJobRepository.save(aiJob);

            // Refund spent coins (only if anything was actually debited)
            try {
                if (coinCost > 0) {
                    log.info("Refunding {} coins to user: {} due to job failure", coinCost, userId);
                    coinWalletService.credit(userId, coinCost, CoinLedgerReason.REFUND, "JOB_FAIL_" + jobId);
                }
            } catch (Exception refundEx) {
                log.error("Failed to refund coins to user {} for job {}", userId, jobId, refundEx);
            }
        }
    }

    private List<CvSection> optimizeSectionsInParallel(
            Long userId, List<CvSectionDto> sections, JobListing job, List<UserSkill> skills) {
        List<CompletableFuture<CvSection>> futures = sections.stream()
            .map(section -> CompletableFuture.supplyAsync(() -> {
                // Acquire before the LLM call so only N sections hit the
                // endpoint at once; release even when the call fails.
                try {
                    sectionPermits.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for section slot", e);
                }
                try {
                    return llmService.optimizeSection(userId, section.name(), section.content(), job, skills);
                } finally {
                    sectionPermits.release();
                }
            }, taskExecutor))
            .toList();

        // Fail loudly (propagates to the job FAILED + refund path) instead of
        // silently fabricating score-0 sections when the LLM is down or the
        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }
}
