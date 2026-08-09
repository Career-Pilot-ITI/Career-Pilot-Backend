package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.service.ILlmService;

import com.careerpilot.backend.dto.response.AtsScore;
import com.careerpilot.backend.dto.response.CoverLetterDraft;
import com.careerpilot.backend.dto.response.CvAnalysis;
import com.careerpilot.backend.dto.response.CvSection;
import com.careerpilot.backend.dto.response.CvSectionDto;
import com.careerpilot.backend.dto.response.CvSectionImprovement;
import com.careerpilot.backend.dto.response.GeneratedQuestion;
import com.careerpilot.backend.dto.response.JobDraft;
import com.careerpilot.backend.dto.response.ScoreResponse;
import com.careerpilot.backend.entity.ENUMs.DocType;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.FeedbackReport;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.QuestionBank;
import com.careerpilot.backend.entity.QuestionScore;
import com.careerpilot.backend.entity.RagContextDocument;
import com.careerpilot.backend.entity.SessionQuestion;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserSkill;

import com.careerpilot.backend.repository.IFeedbackReportRepository;
import com.careerpilot.backend.repository.IQuestionBankRepository;
import com.careerpilot.backend.repository.IQuestionScoreRepository;
import com.careerpilot.backend.repository.IRagContextDocumentRepository;
import com.careerpilot.backend.repository.ISessionQuestionRepository;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.careerpilot.backend.utils.PiiRedactionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LlmServiceImpl implements ILlmService {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper;
  private final IQuestionBankRepository questionBankRepository;
  private final IFeedbackReportRepository feedbackReportRepository;
  private final IRagContextDocumentRepository ragContextDocumentRepository;
  private final ISessionQuestionRepository sessionQuestionRepository;
  private final IQuestionScoreRepository questionScoreRepository;

  @Override
  public ScoreResponse scoreAnswer(Long questionId, Long userId, String transcript) {
    String idealAnswer = buildIdealAnswerContext(questionId);
    String history = buildUserHistoryContext(userId);
    String cv = buildCvContext(userId);
    String sessionCtx = buildSessionContext(userId);

    String prompt = """
        Score this interview answer.

        Question being answered: %s

        Ideal answer reference:
        %s

        Candidate's past performance:
        %s

        Candidate's CV context:
        %s

        Session context:
        %s

        Candidate answer: %s

        Score 0-100 on:
        - Content Relevance: does the answer cover the expected keywords and concepts?
        - Clarity: well-structured, coherent, grammatical?
        - Confidence: hedging language? ("I think", "maybe", self-correction)
        - Filler words: count of "um/uh/like/you know"

        Use the ideal answer reference to determine if the candidate hit the key points.
        Use the past performance and CV to calibrate expectations.

        Return ONLY raw JSON with no markdown formatting.
        {"contentRelevance": 0, "clarity": 0, "confidence": 0, "fillerWords": 0, "reasoning": "..."}
        """.formatted(questionId, idealAnswer, history, cv, sessionCtx, transcript);

    String response = chatClient.prompt()
        .system(s -> s.text(
            """
                You are an expert interview evaluator with 15+ years of experience.
                Score answers against the ideal answer keywords. Be critical — a score of 100 means the answer was perfect, which is rare.
                Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
                """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), ScoreResponse.class);
    } catch (Exception e) {
      log.warn("Failed to parse score response: {}", response, e);
      return new ScoreResponse(0, 0, 0, 0, "Could not evaluate answer");
    }
  }

  @Override
  public List<String> generateSessionTips(Long sessionId, Long userId) {
    List<SessionQuestion> questions = sessionQuestionRepository
        .findBySessionIdOrderByQuestionOrderAsc(sessionId);

    List<Long> questionIds = questions.stream()
        .map(SessionQuestion::getId)
        .collect(Collectors.toList());

    Map<Long, QuestionScore> scoreByQuestionId = questionScoreRepository
        .findBySessionQuestionIdIn(questionIds).stream()
        .collect(Collectors.toMap(score -> score.getSessionQuestion().getId(), score -> score));

    StringBuilder sb = new StringBuilder();
    sb.append("Here is the full interview session. Generate 3-5 overall coaching tips.\n\n");

    for (SessionQuestion q : questions) {
      sb.append("--- Question ").append(q.getQuestionOrder()).append(" ---\n");
      sb.append("Question text: ").append(q.getQuestionText()).append("\n");
      sb.append("Candidate answer: ").append(q.getUserTranscript()).append("\n");

      QuestionScore sc = scoreByQuestionId.get(q.getId());
      if (sc != null) {
        sb.append("Scores: contentRelevance=").append(sc.getContentRelevance())
            .append(", clarity=").append(sc.getClarity())
            .append(", confidence=").append(sc.getConfidence())
            .append(", pacing=").append(sc.getPacing())
            .append(", fillerWords=").append(sc.getFillerWords())
            .append(", overall=").append(sc.getOverallScore()).append("\n");
      }
      sb.append("\n");
    }

    String prompt = sb.toString() + """
        Return ONLY a JSON array of strings with no markdown formatting.
        Example: ["Focus on structuring your answer with STAR method", "Work on reducing filler words"]
        Each tip must be a concrete, actionable recommendation based on the patterns visible in the transcripts and scores.
        """;

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are a senior career coach helping candidates improve their interview performance.
            Provide specific, actionable, and constructive feedback.
            Each tip should be a concrete recommendation, not generic advice.
            Focus on the patterns across the entire session.
            Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), new TypeReference<List<String>>() {});
    } catch (Exception e) {
      log.warn("Failed to parse session tips response: {}", response, e);
      return List.of("Review your answers and focus on clarity and structure.");
    }
  }

  @Override
  public String generateQuestionTip(String questionText, String transcript,
      int contentRelevance, int clarity, int confidence, int pacing, int fillerWords) {
    String prompt = """
        Based on this interview answer and scores, generate a brief coaching tip or praise (1-2 sentences).

        Question: %s
        Candidate answer: %s

        Scores:
        - Content Relevance: %s/100
        - Clarity: %s/100
        - Confidence: %s/100
        - Pacing: %s/100
        - Filler words: %s/100

        If the overall score is high (80+), praise the candidate and suggest one minor improvement.
        If the overall score is low, give one specific, actionable tip to improve.
        Return ONLY the tip text, no JSON, no markdown.
        """.formatted(questionText, transcript,
        contentRelevance, clarity, confidence, pacing, fillerWords);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are a senior career coach giving real-time feedback after each interview question.
            Be concise (1-2 sentences). Be specific. Be encouraging even when being critical.
            Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
            """))
        .user(prompt)
        .call()
        .content();

    return response != null ? response.strip() : "Keep practicing to improve your answer.";
  }

  @Override
  public CvAnalysis analyzeCv(String cvText) {
    String prompt = """
        Extract structured information from this CV text.

        CV: %s

        Return ONLY raw JSON with no markdown formatting.
        {"skills": [], "yearsOfExperience": 0, "targetRole": "", "educationLevel": "", "displayName": "", "currentJobTitle": ""}
        """
        .formatted(cvText);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are an HR expert specializing in CV parsing and skills assessment.
            Extract structured information from unstructured CV text accurately.
            If a field cannot be determined, use reasonable defaults or empty lists.
            Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), CvAnalysis.class);
    } catch (Exception e) {
      log.warn("Failed to parse CV analysis response: {}", response, e);
      return new CvAnalysis();
    }
  }

  @Override
  public JobDraft parseJobPosting(String rawText) {
    String trimmed = rawText == null ? "" : rawText.strip();
    if (trimmed.length() > 12000) {
      trimmed = trimmed.substring(0, 12000);
    }

    String prompt = """
        Extract structured job information from this raw job posting text.

        Job posting text:
        %s

        Return ONLY raw JSON with no markdown formatting, using this exact shape:
        {
          "title": "",
          "companyName": "",
          "location": "",
          "description": "",
          "employmentType": "",
          "seniorityLevel": "",
          "requiredSkills": [],
          "preferredSkills": [],
          "technologies": [],
          "salaryMin": null,
          "salaryMax": null,
          "currency": "",
          "experienceYears": null,
          "educationLevel": ""
        }
        """
        .formatted(trimmed);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are a recruiter-grade job parsing expert.
            Extract structured fields from unstructured job posting text accurately.

            Rules:
            - title: the exact job title as printed (e.g. "Senior Backend Engineer").
            - companyName: the employer. Leave null when only a recruitment agency is named.
            - location: city + country as printed; null if remote and no city is given.
            - description: a clean, lightly condensed version of the full posting body
              (responsibilities + requirements). Preserve all technical details and
              specific numbers. Strip repeated boilerplate lines.
            - employmentType: exactly one of FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP, or null.
            - seniorityLevel: exactly one of JUNIOR, MID, SENIOR, LEAD, or null.
            - requiredSkills: hard requirements explicitly listed as must-have
              (technologies, languages, frameworks, tools, certifications). 5-15 items.
            - preferredSkills: nice-to-have skills. Leave empty if the posting does not
              distinguish required vs preferred.
            - technologies: the concrete tech stack mentioned anywhere (languages,
              frameworks, databases, platforms, cloud providers). Deduplicate.
            - salaryMin/salaryMax: annual figures in the posting's currency when the
              posting states a salary or range; otherwise null. Convert "120k" to 120000.
            - currency: ISO 4217 code (USD, EUR, EGP, ...) or null.
            - experienceYears: the minimum years of experience demanded, else null.
            - educationLevel: degree or certification required, else null.

            Never invent fields that are not in the text. Use null or empty lists when a
            field is absent. Preserve any [REDACTED:...] placeholder verbatim if it appears in
            the text; never guess, fill in, or remove it. Return ONLY the JSON object.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), JobDraft.class);
    } catch (Exception e) {
      log.warn("Failed to parse job posting response: {}", response, e);
      return new JobDraft(
          null, null, null,
          trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed,
          null, null, List.of(), List.of(), List.of(),
          null, null, null, null, null);
    }
  }

  @Override
  public AtsScore scoreCv(String cvText, JobListing job) {
    String jobCtx = buildJobContext(job);
    String cv = cvText == null ? "" : cvText.strip();

    String prompt = """
        Score how relevant this candidate's CV is to the job posting. Be detailed and specific.

        JOB POSTING:
        %s

        CANDIDATE CV:
        %s

        Return ONLY raw JSON with no markdown formatting, using this exact shape:
        {
          "overallScore": 0,
          "matchPercentage": 0,
          "matchedSkills": [],
          "missingRequiredSkills": [],
          "missingPreferredSkills": [],
          "strengths": [],
          "weaknesses": [],
          "sections": [
            {"section": "Experience", "score": 0, "feedback": ""}
          ],
          "recommendations": []
        }

        Guidelines:
        - overallScore: 0-100 overall CV-to-job fit.
        - matchPercentage: how much of the job's required skill set the CV covers.
        - matchedSkills: job skills explicitly present in the CV.
        - missingRequiredSkills: required skills absent from the CV.
        - missingPreferredSkills: preferred skills absent from the CV.
        - strengths/weaknesses: specific, evidence-based, tied to the posting.
        - sections: evaluate CV sections (Summary, Experience, Education, Skills, Projects) individually.
        - recommendations: concrete, prioritized edits the candidate can make to improve the score.
        Base every claim on the actual CV text. Do not invent skills.
        """
        .formatted(jobCtx, cv);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are an expert ATS (Applicant Tracking System) reviewer and recruiter.
            Evaluate CV-to-job relevance precisely. Be critical but constructive.
            Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), AtsScore.class);
    } catch (Exception e) {
      log.warn("Failed to parse ATS score response: {}", response, e);
      return new AtsScore();
    }
  }

  @Override
  public List<CvSectionDto> splitCvIntoSections(Long userId, String cvText) {
    if (cvText == null || cvText.isBlank()) {
      return List.of();
    }

    String prompt = """
        Split the following CV into its primary logical sections (e.g., Professional Summary, Work Experience, Projects, Technical Skills, Education).
        For each section, extract the section name and its raw content. Preserve the content exactly as it is written in the CV.
        
        CV CONTENT:
        %s
        
        Return ONLY a raw JSON array of objects with no markdown code blocks, using this exact shape:
        [
          {"name": "Section Name", "content": "Raw section content here"}
        ]
        """.formatted(cvText);

    try {
      String response = chatClient.prompt()
          .system("You are an expert ATS parser. Split the CV into logical sections. Return only a raw JSON array.")
          .user(prompt)
          .call()
          .content();

      List<CvSectionDto> sections = objectMapper.readValue(
          stripMarkdown(response),
          new TypeReference<List<CvSectionDto>>() {}
      );
      return sections != null ? sections : List.of();
    } catch (Exception e) {
      log.warn("Failed to split CV into sections: {}", e.getMessage());
      return List.of(new CvSectionDto("Full Profile", cvText));
    }
  }

  @Override
  public CvSection optimizeSection(
      Long userId, String sectionName, String sectionContent, JobListing job, List<UserSkill> skills
  ) {
    if (sectionContent == null || sectionContent.isBlank()) {
      return new CvSection(sectionName, 100, List.of());
    }

    String jobCtx = buildJobContext(job);
    String skillCtx = buildSkillsContext(skills);

    String prompt = """
        Optimize the following CV section for the given job posting, taking the candidate's validated skills into account.
        Identify specific parts of the section that can be improved. Do NOT rewrite the entire section.
        
        JOB POSTING:
        %s
        
        CANDIDATE VALIDATED SKILLS:
        %s
        
        CV SECTION NAME: %s
        CV SECTION CONTENT:
        %s
        
        Return ONLY a raw JSON object with no markdown code blocks, using this exact shape:
        {
          "score": 75,
          "improvements": [
            {
              "original": "EXACT verbatim substring from CV SECTION CONTENT to replace",
              "improved": "The suggested improvement using active verbs and quantified results",
              "reason": "Why this change improves the ATS score"
            }
          ]
        }
        
        Rules:
        1. "score" must be an integer representing the current ATS fit score of this section (0-100).
        2. "original" MUST be an exact, case-sensitive, verbatim substring from the provided CV SECTION CONTENT.
        3. Keep every real fact (dates, companies, titles) intact.
        4. Preserve any [REDACTED:...] placeholder inside the original text verbatim in "improved". Never guess, fill, or drop placeholders.
        """.formatted(jobCtx, skillCtx, sectionName, sectionContent);

    try {
      String response = chatClient.prompt()
          .system("You are a professional ATS optimizer. Return only the requested JSON for the section.")
          .user(prompt)
          .call()
          .content();

      // Read response into a temporary holder
      CvSection section = objectMapper.readValue(
          stripMarkdown(response),
          CvSection.class
      );

      // Validate improvements
      List<CvSectionImprovement> validatedImprovements = section.improvements().stream()
          .filter(imp -> {
            String original = imp.original() == null ? "" : imp.original().trim();
            if (original.isEmpty()) {
              return false;
            }
            // 1. Must exist verbatim in sectionContent
            if (!sectionContent.contains(original)) {
              log.warn("Skipping improvement - original anchor not found verbatim in CV section: '{}'", original);
              return false;
            }
            // 2. Protect [REDACTED:...] placeholders
            if (original.contains("[REDACTED:") && !imp.improved().contains("[REDACTED:")) {
              log.warn("Skipping improvement - drops redacted placeholder: '{}'", original);
              return false;
            }
            return true;
          })
          .toList();

      return new CvSection(section.name() != null ? section.name() : sectionName, section.score(), validatedImprovements);
    } catch (Exception e) {
      log.warn("Failed to optimize CV section '{}': {}", sectionName, e.getMessage());
      return new CvSection(sectionName, 100, List.of());
    }
  }

  @Override
  public List<String> recommendTracks(Long userId, String cvText, List<Track> tracks) {
    if (cvText == null || cvText.isBlank() || tracks == null || tracks.isEmpty()) {
      return List.of();
    }

    String trackCtx = buildTracksContext(tracks);
    String prompt = """
        Analyze the candidate's CV and recommend up to 3 matching tracks from the list of available tracks.
        
        AVAILABLE TRACKS:
        %s
        
        CV CONTENT:
        %s
        
        Return ONLY a raw JSON array of track names. The names MUST match the available tracks exactly.
        Example: ["Track Name 1", "Track Name 2"]
        """.formatted(trackCtx, cvText);

    try {
      String response = chatClient.prompt()
          .system("You recommend matching tracks from the provided list. Return only a raw JSON array of strings.")
          .user(prompt)
          .call()
          .content();

      List<String> recommendations = objectMapper.readValue(
          stripMarkdown(response),
          new TypeReference<List<String>>() {}
      );
      return recommendations != null ? recommendations : List.of();
    } catch (Exception e) {
      log.warn("Failed to generate track recommendations: {}", e.getMessage());
      return List.of();
    }
  }

  @Override
  public CoverLetterDraft generateCoverLetter(String cvText, JobListing job, UserProfile profile,
      String companyResearch, SubscriptionTier tier) {
    String jobCtx = buildJobContext(job);
    String cv = cvText == null ? "" : cvText.strip();
    String profileCtx = buildProfileContext(profile);
    String research = companyResearch == null || companyResearch.isBlank()
        ? "No company research available."
        : companyResearch.strip();
    String researchGuidance = buildTierResearchGuidance(tier);

    String prompt = """
        Write a personalized cover letter for this job application, plus advice on the best way
        to approach the company.

        JOB POSTING:
        %s

        CANDIDATE CV:
        %s

        CANDIDATE PROFILE:
        %s

        COMPANY RESEARCH:
        %s

        RESEARCH GUIDANCE:
        %s

        Return ONLY raw JSON with no markdown formatting, using this exact shape:
        {
          "coverLetter": "...",
          "approachTips": "..."
        }

        Guidelines:
        - coverLetter: 3-4 short paragraphs, addressed to the hiring team, tailored to the job and
          company. Reference the candidate's strongest, job-relevant facts from the CV. Only cite
          company facts that appear in the COMPANY RESEARCH. Never invent company facts.
        - approachTips: 3-5 concrete, actionable tips on the best way to approach this company
          (channel, timing, what to highlight, follow-up), grounded in the research where possible.
        """
        .formatted(jobCtx, cv, profileCtx, research, researchGuidance);

    String response = chatClient.prompt()
        .system(s -> s.text("""
            You are an expert career coach and cover letter writer.
            Match the candidate's strengths to the company's needs. Stay truthful about both the
            candidate and the company. The company research may be limited or unreliable — follow
            the research guidance carefully and never invent facts.
            Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.
            """))
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(stripMarkdown(response), CoverLetterDraft.class);
    } catch (Exception e) {
      log.warn("Failed to parse cover letter response: {}", response, e);
      return new CoverLetterDraft();
    }
  }

  
  private String buildJobContext(JobListing job) {
    if (job == null)
      return "No job posting available.";
    StringBuilder sb = new StringBuilder();
    sb.append("Title: ").append(nvl(job.getTitle())).append("\n");
    sb.append("Company: ").append(nvl(job.getCompanyName())).append("\n");
    sb.append("Location: ").append(nvl(job.getLocation())).append("\n");
    sb.append("Employment type: ").append(nvl(job.getEmploymentType())).append("\n");
    sb.append("Seniority: ").append(nvl(job.getSeniorityLevel())).append("\n");
    sb.append("Required skills: ").append(job.getRequiredSkills() != null
        ? String.join(", ", job.getRequiredSkills()) : "").append("\n");
    sb.append("Preferred skills: ").append(job.getPreferredSkills() != null
        ? String.join(", ", job.getPreferredSkills()) : "").append("\n");
    sb.append("Technologies: ").append(job.getTechnologies() != null
        ? String.join(", ", job.getTechnologies()) : "").append("\n");
    sb.append("Responsibilities:\n").append(nvl(job.getResponsibilities())).append("\n");
    sb.append("Qualifications:\n").append(nvl(job.getQualifications())).append("\n");
    sb.append("Description:\n").append(nvl(job.getDescription())).append("\n");
    return sb.toString();
  }

  private String buildSkillsContext(List<UserSkill> skills) {
    if (skills == null || skills.isEmpty())
      return "No validated skills yet.";
    return skills.stream()
        .map(s -> "- " + nvl(s.getSkillName()) + ": " + nvl(s.getPerformanceScore()) + "/100, assessed "
            + nvl(s.getTimesAssessed()) + " time(s)")
        .collect(Collectors.joining("\n"));
  }

  private String buildTracksContext(List<Track> tracks) {
    if (tracks == null || tracks.isEmpty())
      return "No tracks available.";
    return tracks.stream()
        .map(t -> "- " + nvl(t.getName()) + (t.getDescription() != null && !t.getDescription().isBlank()
            ? ": " + t.getDescription() : ""))
        .collect(Collectors.joining("\n"));
  }

  private String buildProfileContext(UserProfile profile) {
    if (profile == null)
      return "No profile available.";
    StringBuilder sb = new StringBuilder();
    sb.append("Name: ").append(nvl(profile.getDisplayName())).append("\n");
    sb.append("Target role: ").append(nvl(profile.getTargetRole())).append("\n");
    sb.append("Current job title: ").append(nvl(profile.getCurrentJobTitle())).append("\n");
    sb.append("Years of experience: ").append(nvl(profile.getYearsOfExperience())).append("\n");
    sb.append("Education level: ").append(nvl(profile.getEducationLevel())).append("\n");
    sb.append("Industry: ").append(nvl(profile.getIndustry())).append("\n");
    sb.append("Experience level: ").append(nvl(profile.getExperienceLevel())).append("\n");
    sb.append("Target companies: ").append(nvl(profile.getTargetCompanies())).append("\n");
    return sb.toString();
  }

  private String buildTierResearchGuidance(SubscriptionTier tier) {
    if (tier == null)
      tier = SubscriptionTier.FREE;
    return switch (tier) {
      case PRO -> "The company research is extensive and reliable (agent performed many searches). "
          + "Use specific company facts such as projects, funding, and values freely.";
      case PLUS -> "The company research is a single trimmed search result. You may use the facts "
          + "it contains, but do not over-claim specifics that are not present.";
      case FREE -> "The company research is limited and may be unreliable. Base the letter mostly "
          + "on the CV and job posting; only use company facts that clearly appear in the research.";
    };
  }

  private String nvl(Object value) {
    return value == null ? "" : value.toString();
  }

  private String buildIdealAnswerContext(Long questionId) {
    Optional<QuestionBank> opt = questionBankRepository.findById(questionId);
    if (opt.isEmpty())
      return "No reference answer available.";
    QuestionBank q = opt.get();
    StringBuilder sb = new StringBuilder();
    sb.append("Question: ").append(q.getQuestionText()).append("\n");
    if (q.getExpectedKeywords() != null && !q.getExpectedKeywords().isBlank())
      sb.append("Expected keywords: ").append(q.getExpectedKeywords()).append("\n");
    sb.append("Difficulty: ").append(q.getDifficultyLevel()).append("\n");
    sb.append("Category: ").append(q.getCategory());
    return sb.toString();
  }

  private String buildUserHistoryContext(Long userId) {
    List<FeedbackReport> reports = feedbackReportRepository
        .findBySessionUserIdOrderByCreatedAtDesc(userId);
    if (reports.isEmpty())
      return "No past session history available.";

    double avgScore = reports.stream()
        .mapToInt(FeedbackReport::getOverallScore)
        .average()
        .orElse(0);

    List<String> tips = reports.stream()
        .filter(r -> r.getCoachingTips() != null)
        .map(FeedbackReport::getCoachingTips)
        .toList();

    StringBuilder sb = new StringBuilder();
    sb.append("Past sessions: ").append(reports.size()).append("\n");
    sb.append("Average score: ").append(String.format("%.1f", avgScore)).append("\n");
    if (!tips.isEmpty()) {
      sb.append("Recurring advice:\n");
      tips.stream().limit(3).forEach(t -> sb.append("- ").append(t).append("\n"));
    }
    return sb.toString();
  }

  private String buildCvContext(Long userId) {
    List<RagContextDocument> docs = ragContextDocumentRepository
        .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.CV_EXTRACT);
    if (docs.isEmpty())
      return "No CV context available.";
    return PiiRedactionUtil.redact(docs.get(0).getContent());
  }

  private String buildSessionContext(Long userId) {
    List<RagContextDocument> docs = ragContextDocumentRepository
        .findByUserIdAndDocTypeOrderByCreatedAtDesc(userId, DocType.SESSION_SUMMARY);
    if (docs.isEmpty())
      return "No session context available.";
    return PiiRedactionUtil.redact(docs.stream()
        .limit(3)
        .map(RagContextDocument::getContent)
        .collect(Collectors.joining("\n---\n")));
  }

  private String stripMarkdown(String raw) {
    if (raw == null)
      return "{}";
    return raw.replaceAll("```(?:json)?\\s*", "").trim();
  }

  private String normalizeCv(String cv) {
    return cv.replaceAll("\\s+", " ").trim();
  }

}
