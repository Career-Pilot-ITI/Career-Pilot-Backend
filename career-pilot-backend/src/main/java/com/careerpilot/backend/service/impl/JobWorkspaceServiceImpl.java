package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.JobScrapeException;
import com.careerpilot.backend.dto.request.ImportJobTextRequest;
import com.careerpilot.backend.dto.request.ImportJobUrlRequest;
import com.careerpilot.backend.dto.response.ChocoDataJobResponse;
import com.careerpilot.backend.dto.response.JobDraft;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.ENUMs.JobSourceType;
import com.careerpilot.backend.entity.ENUMs.JobWorkspaceStatus;
import com.careerpilot.backend.entity.JobWorkspace;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.IJobWorkspaceRepository;
import com.careerpilot.backend.service.IJobWorkspaceService;
import com.careerpilot.backend.service.ILinkedInJobScraperService;
import com.careerpilot.backend.service.ISessionQuotaService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerpilot.backend.dto.response.JobWorkspaceResponse;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.repository.IJobListingRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.service.ILlmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorkspaceServiceImpl implements IJobWorkspaceService {
  private final ILlmService llmService;
  private final IUserRepository userRepository;
  private final IJobListingRepository jobListingRepository;
  private final IJobWorkspaceRepository jobWorkspaceRepository;
  private final ILinkedInJobScraperService linkedInJobScraperService;
  private final ISessionQuotaService sessionQuotaService;

  @Value("${app.job-parse.coin-cost:1}")
  private int jobParseCoinCost;

  @Override
  @Transactional
  public JobWorkspaceResponse importFromText(Long userId, ImportJobTextRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    Optional<JobListing> jobListing = jobListingRepository.findBySourceUrl(request.getSourceUrl());
    JobListing job;
    if(jobListing.isPresent()) {
      job = jobListing.get();
    }else {
      JobDraft draft = parseWithEntitlement(userId, request.getDescriptionText());

      job = JobListing.fromDraft(draft, user, JobSourceType.MANUAL_TEXT, request.getSourceUrl());
      job = jobListingRepository.save(job);
    }
    JobWorkspace workspace = JobWorkspace.builder()
        .user(user)
        .job(job)
        .status(JobWorkspaceStatus.SAVED)
        .build();
    workspace = jobWorkspaceRepository.save(workspace);

    return JobWorkspaceResponse.from(workspace);
  }

  @Override
  @Transactional
  public JobWorkspaceResponse importFromUrl(Long userId, ImportJobUrlRequest request) {
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    Optional<JobListing> jobListing = jobListingRepository.findBySourceUrl(request.getUrl());
    JobListing job;
    if (jobListing.isPresent()) {
      job = jobListing.get();
    } else {
      ChocoDataJobResponse response;
      try {
        response = linkedInJobScraperService.scrape(request.getUrl());
      } catch (JobScrapeException e) {
        throw e.suggest(
            "Couldn't fetch this job automatically. Try pasting the job description text instead ");
      }
      job = JobListing.fromChocoData(response, user, request.getUrl());
      if (response.description() != null && !response.description().isBlank()) {
        enrichWithParsedSkills(userId, job, response.description());
      }
      job = jobListingRepository.save(job);
    }
    JobWorkspace workspace = JobWorkspace.builder()
        .user(user)
        .job(job)
        .status(JobWorkspaceStatus.SAVED)
        .build();
    workspace = jobWorkspaceRepository.save(workspace);

    return JobWorkspaceResponse.from(workspace);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobWorkspaceResponse> listWorkspaces(Long userId) {
    return jobWorkspaceRepository.findByUserIdOrderByCreatedAtDesc(userId)
        .stream()
        .map(JobWorkspaceResponse::from)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public JobWorkspaceResponse getWorkspace(Long id, Long userId) {
    JobWorkspace workspace = jobWorkspaceRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
    return JobWorkspaceResponse.from(workspace);
  }

  @Override
  @Transactional
  public void deleteWorkspace(Long id, Long userId) {
    JobWorkspace workspace = jobWorkspaceRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
    jobWorkspaceRepository.delete(workspace);
  }

  @Override
  @Transactional
  public JobWorkspaceResponse updateStatus(Long id, Long userId, JobWorkspaceStatus status) {
    JobWorkspace workspace = jobWorkspaceRepository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
    workspace.setStatus(status);
    workspace = jobWorkspaceRepository.save(workspace);
    return JobWorkspaceResponse.from(workspace);
  }

  private JobDraft parseWithEntitlement(Long userId, String rawText) {
    if (sessionQuotaService.isPremium(userId)) {
      return llmService.parseJobPosting(rawText);
    }
    if (sessionQuotaService.tryDebit(userId, jobParseCoinCost, CoinLedgerReason.JOB_PARSE)) {
      return llmService.parseJobPosting(rawText);
    }
    log.info("User {} has no coins for job parse storing raw text", userId);
    String fallbackText = rawText != null && rawText.length() > 2000 ? rawText.substring(0, 2000) : rawText;
    return new JobDraft(null, null, null, fallbackText, null, null, List.of(), List.of(), List.of(),
        null, null, null, null, null);
  }

  private void enrichWithParsedSkills(Long userId, JobListing job, String description) {
    JobDraft draft = parseWithEntitlement(userId, description);
    if (draft.requiredSkills() != null && !draft.requiredSkills().isEmpty()) {
      job.setRequiredSkills(draft.requiredSkills());
    }
    if (draft.preferredSkills() != null && !draft.preferredSkills().isEmpty()) {
      job.setPreferredSkills(draft.preferredSkills());
    }
    if (draft.technologies() != null && !draft.technologies().isEmpty()) {
      job.setTechnologies(draft.technologies());
    }
  }

}
