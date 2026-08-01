package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.controller.advice.WorkspaceException;
import com.careerpilot.backend.dto.response.AtsScore;
import com.careerpilot.backend.dto.response.AtsScoreResponse;
import com.careerpilot.backend.dto.response.CoverLetterDraft;
import com.careerpilot.backend.dto.response.CoverLetterResponse;
import com.careerpilot.backend.dto.response.CvOptimization;
import com.careerpilot.backend.dto.response.CvOptimizationResponse;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.ENUMs.DocType;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.JobWorkspace;
import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.repository.IJobWorkspaceRepository;
import com.careerpilot.backend.repository.IRagContextDocumentRepository;
import com.careerpilot.backend.repository.ISubscriptionRepository;
import com.careerpilot.backend.repository.ITrackRepository;
import com.careerpilot.backend.repository.IUserProfileRepository;
import com.careerpilot.backend.repository.IUserSkillRepository;
import com.careerpilot.backend.service.IJobWorkspaceAiService;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.service.ISessionQuotaService;
import com.careerpilot.backend.service.agent.CoverLetterAgentService;
import com.careerpilot.backend.service.agent.WebSearchService;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobWorkspaceAiServiceImpl implements IJobWorkspaceAiService {

  private static final int FREE_RESEARCH_MAX_CHARS = 2000;
  private static final Pattern COMPANY_NAME_NOISE = Pattern.compile(
      "(?i)\\b(recruitment|recruiting|staffing|agency|jobs|job|careers|hiring|talent|ltd|llc|inc)\\b",
      Pattern.UNICODE_CASE);

  private final IJobWorkspaceRepository jobWorkspaceRepository;
  private final IRagContextDocumentRepository ragContextDocumentRepository;
  private final IUserSkillRepository userSkillRepository;
  private final ITrackRepository trackRepository;
  private final IUserProfileRepository userProfileRepository;
  private final ISubscriptionRepository subscriptionRepository;

  private final ILlmService llmService;
  private final ISessionQuotaService sessionQuotaService;
  private final CoverLetterAgentService coverLetterAgentService;

  private final WebSearchService webSearchService;

  @Value("${app.ats-score.coin-cost:2}")
  private int atsScoreCoinCost;

  @Value("${app.cv-optimize.coin-cost:2}")
  private int optimizeCvCoinCost;

  @Value("${app.cover-letter.coin-cost:2}")
  private int coverLetterCoinCost;

  @Override
  @Transactional
  public AtsScoreResponse scoreCv(Long workspaceId, Long userId) {
    JobWorkspace workspace = requireWorkspace(workspaceId, userId);
    String cvText = latestCvText(userId);
    int coinCost = requireEntitled(userId, atsScoreCoinCost, CoinLedgerReason.ATS_SCORE);

    AtsScore score = llmService.scoreCv(cvText, workspace.getJob());

    LocalDateTime updatedAt = LocalDateTime.now();
    workspace.setCvScore(score.overallScore());
    workspace.setCvScoreUpdatedAt(updatedAt);
    jobWorkspaceRepository.save(workspace);

    return AtsScoreResponse.from(score, coinCost, updatedAt);
  }

  @Override
  @Transactional
  public CvOptimizationResponse optimizeCv(Long workspaceId, Long userId) {
    JobWorkspace workspace = requireWorkspace(workspaceId, userId);
    String cvText = latestCvText(userId);
    int coinCost = requireEntitled(userId, optimizeCvCoinCost, CoinLedgerReason.CV_OPTIMIZE);

    List<UserSkill> skills = userSkillRepository.findByUserId(userId);
    List<Track> tracks = trackRepository.findByIsActiveTrue();
    CvOptimization optimized = llmService.optimizeCv(cvText, workspace.getJob(), skills, tracks);

    workspace.setCvOptimizedText(optimized.optimizedCv());
    jobWorkspaceRepository.save(workspace);

    return CvOptimizationResponse.from(optimized, coinCost);
  }

  @Override
  @Transactional
  public CoverLetterResponse generateCoverLetter(Long workspaceId, Long userId) {
    JobWorkspace workspace = requireWorkspace(workspaceId, userId);
    String cvText = latestCvText(userId);
    int coinCost = requireEntitled(userId, coverLetterCoinCost, CoinLedgerReason.COVER_LETTER);

    UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
    SubscriptionTier tier = resolveTier(userId);

    CoverLetterDraft draft;
    if (tier == SubscriptionTier.FREE) {
      String research = serviceSideCompanyResearch(workspace);
      draft = llmService.generateCoverLetter(cvText, workspace.getJob(), profile, research, tier);
    } else {
      draft = coverLetterAgentService.researchAndWrite(tier, workspace.getJob(), cvText, profile);
    }

    workspace.setCoverLetterText(draft.coverLetter());
    jobWorkspaceRepository.save(workspace);

    return CoverLetterResponse.from(draft, coinCost);
  }

  private JobWorkspace requireWorkspace(Long workspaceId, Long userId) {
    return jobWorkspaceRepository.findByIdAndUserId(workspaceId, userId)
        .orElseThrow(() -> new WorkspaceException.WorkspaceNotFoundException(
            "Workspace not found: " + workspaceId));
  }

  private String latestCvText(Long userId) {
    List<RagContextDocument> docs = ragContextDocumentRepository
        .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.CV_EXTRACT);
    if (docs.isEmpty()) {
      throw new WorkspaceException.CvNotFoundException(
          "No CV found. Please upload your CV first.");
    }
    return PiiRedactionUtil.redact(docs.get(0).getContent());
  }

  private int requireEntitled(Long userId, int cost, CoinLedgerReason reason) {
    if (sessionQuotaService.isPremium(userId)) {
      return 0;
    }
    if (sessionQuotaService.tryDebit(userId, cost, reason)) {
      return cost;
    }
    throw new WalletException.InsufficientBalanceException(
        "Not enough coins. Subscribe or buy coins.");
  }

  private SubscriptionTier resolveTier(Long userId) {
    return subscriptionRepository.findByUserId(userId)
        .map(s -> s.getTier())
        .orElse(SubscriptionTier.FREE);
  }

  private String serviceSideCompanyResearch(JobWorkspace workspace) {
    String company = workspace.getJob().getCompanyName();
    if (company == null || company.isBlank()) {
      return "No company name available.";
    }
    String sanitized = COMPANY_NAME_NOISE.matcher(company).replaceAll(" ").strip();
    String query = "What are the main pillars, core values, projects, and funding of the company "
        + sanitized + "?";
    String results = webSearchService.search(query);
    if (results == null || results.isBlank()) {
      return "No company research available.";
    }
    return results.length() > FREE_RESEARCH_MAX_CHARS
        ? results.substring(0, FREE_RESEARCH_MAX_CHARS) : results;
  }
}
