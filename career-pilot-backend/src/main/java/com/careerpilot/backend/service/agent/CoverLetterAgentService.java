package com.careerpilot.backend.service.agent;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.annotation.RedactPii;
import com.careerpilot.backend.dto.response.CoverLetterDraft;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.JobListing;
import com.careerpilot.backend.entity.UserProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CoverLetterAgentService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final CompanyResearchTools companyResearchTools;

    @Value("${app.cover-letter.plus-search-max-chars:1500}")
    private int plusSearchMaxChars;

    @RateLimit
    @RedactPii
    public CoverLetterDraft researchAndWrite(
            SubscriptionTier tier, JobListing job, String cvText, UserProfile profile) {
        boolean allowManySearches = tier == SubscriptionTier.PRO;
        String researchDirective = allowManySearches
                ? "You may call searchCompany AS MANY TIMES AS YOU NEED (funding, core values, projects, recent news). "
                        + "Gather rich, reliable company context before writing."
                : "You may call searchCompany AT MOST ONCE, and pass maxChars=" + plusSearchMaxChars
                        + " to keep the result short. Then write the letter from that single search plus the job and CV.";

        String prompt = """
                Write a personalized cover letter for this job application, plus advice on the best
                way to approach the company.

                JOB POSTING:
                %s

                CANDIDATE CV:
                %s

                CANDIDATE PROFILE:
                %s

                RESEARCH DIRECTIVE:
                %s

                Use the searchCompany tool to research the company. Only cite company facts that
                actually appear in the search results. Never invent company facts.

                Return ONLY raw JSON with no markdown formatting:
                {"coverLetter": "...", "approachTips": "..."}

                - coverLetter: 3-4 short paragraphs, tailored to the job and company, referencing the
                  candidate's strongest job-relevant facts from the CV.
                - approachTips: 3-5 concrete tips on the best way to approach this company
                  (channel, timing, what to highlight, follow-up), grounded in the research where possible.
                """.formatted(buildJobContext(job), cvText != null ? cvText : "No CV available.",
                buildProfileContext(profile), researchDirective);

        String response = chatClient.prompt()
                .system("You are an expert career coach and cover letter writer. "
                        + "Match the candidate's strengths to the company's needs. Stay truthful about the candidate and the company. "
                        + "Any [REDACTED:...] placeholder in the input is protected PII — preserve it verbatim in anything you output; never fill it in, guess it, or remove it.")
                .user(prompt)
                .tools(companyResearchTools)
                .call()
                .content();

        try {
            return objectMapper.readValue(
                    response != null ? response.replaceAll("```(?:json)?\\s*", "").trim() : "{}",
                    CoverLetterDraft.class);
        } catch (Exception e) {
            log.warn("Failed to parse cover letter agent response: {}", response, e);
            return new CoverLetterDraft();
        }
    }

    private String buildJobContext(JobListing job) {
        if (job == null) return "No job posting available.";
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
        return sb.toString();
    }

    private String buildProfileContext(UserProfile profile) {
        if (profile == null) return "No profile available.";
        StringBuilder sb = new StringBuilder();
        sb.append("Name: ").append(nvl(profile.getDisplayName())).append("\n");
        sb.append("Target role: ").append(nvl(profile.getTargetRole())).append("\n");
        sb.append("Current job title: ").append(nvl(profile.getCurrentJobTitle())).append("\n");
        sb.append("Years of experience: ").append(nvl(profile.getYearsOfExperience())).append("\n");
        sb.append("Education level: ").append(nvl(profile.getEducationLevel())).append("\n");
        sb.append("Industry: ").append(nvl(profile.getIndustry())).append("\n");
        return sb.toString();
    }

    private String nvl(Object value) {
        return value == null ? "" : value.toString();
    }
}
