package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.response.*;
import com.careerpilot.backend.entity.AiJob;
import com.careerpilot.backend.entity.ENUMs.AiJobStatus;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.repository.IAiJobRepository;
import com.careerpilot.backend.repository.IJobWorkspaceRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.repository.IUserSkillRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Async("taskExecutor")
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

            // Step 1: Redact CV PII before sending to LLM
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

            // Step 3: Optimize sections one-by-one to prevent size/parsing errors
            List<UserSkill> skills = userSkillRepository.findByUserId(userId);
            List<CvSection> optimizedSections = new ArrayList<>();

            int totalSections = parsedSections.size();
            for (int i = 0; i < totalSections; i++) {
                CvSectionDto section = parsedSections.get(i);
                String stepMsg = String.format("Optimizing section '%s' (%d/%d)...", section.name(), i + 1, totalSections);
                log.info("Job {}: {}", jobId, stepMsg);

                aiJob.setCurrentStep(stepMsg);
                int progress = 20 + (int) (((double) i / totalSections) * 60); // range from 20% to 80%
                aiJob.setProgressPercentage(progress);
                aiJobRepository.save(aiJob);

                CvSection optimizedSec = llmService.optimizeSection(userId, section.name(), section.content(), job, skills);
                optimizedSections.add(optimizedSec);
            }

            // Step 4: Recommend matching tracks
            aiJob.setCurrentStep("Generating matching career tracks...");
            aiJob.setProgressPercentage(90);
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
}
