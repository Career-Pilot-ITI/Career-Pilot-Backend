package com.careerpilot.backend.entity;

import com.careerpilot.backend.dto.response.ChocoDataJobResponse;
import com.careerpilot.backend.dto.response.JobDraft;
import com.careerpilot.backend.entity.ENUMs.JobSourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // many job to single user (who creates the job but also the job is shared with other users)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "description")
    private String description;

    @Column(name = "employment_type", length = 50)
    private String employmentType;

    @Column(name = "seniority_level", length = 50)
    private String seniorityLevel;

    //used json for storing required and preferred skills
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "required_skills", nullable = false)
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_skills", nullable = false)
    @Builder.Default
    private List<String> preferredSkills = new ArrayList<>();

    @Column(name = "responsibilities")
    private String responsibilities;

    @Column(name = "qualifications")
    private String qualifications;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "technologies", nullable = false)
    @Builder.Default
    private List<String> technologies = new ArrayList<>();

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "education_level", length = 100)
    private String educationLevel;

    @Column(name = "application_url", length = 500)
    private String applicationUrl;

    // Cache/dedup key 
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "source_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private JobSourceType sourceType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "company_logo_url", length = 1024)
    private String companyLogoUrl;

    @Column(name = "posted_label", length = 100)
    private String postedLabel;

    @Column(name = "applicants_label", length = 100)
    private String applicantsLabel;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Build a transient JobListing from an LLM-parsed draft. The owner, source
     * type, and source URL are set by the caller (they are not part of the
     * LLM contract).
     */
    public static JobListing fromDraft(JobDraft d, User user, JobSourceType sourceType, String sourceUrl) {
        return JobListing.builder()
                .user(user)
                .title(d.title())
                .companyName(d.companyName())
                .location(d.location())
                .description(d.description())
                .employmentType(d.employmentType())
                .seniorityLevel(d.seniorityLevel())
                .requiredSkills(d.requiredSkills())
                .preferredSkills(d.preferredSkills())
                .technologies(d.technologies())
                .salaryMin(d.salaryMin())
                .salaryMax(d.salaryMax())
                .currency(d.currency())
                .experienceYears(d.experienceYears())
                .educationLevel(d.educationLevel())
                .sourceUrl(sourceUrl)
                .sourceType(sourceType)
                .build();
    }

    /**
     * Build a transient JobListing directly from a ChocoData response (no LLM
     * round-trip — ChocoData already returns structured fields). The owner and
     * source type are set by the caller.
     */
    public static JobListing fromChocoData(ChocoDataJobResponse r, User user, String sourceUrl) {
        ParsedSalary salary = parseSalary(r.salary());
        return JobListing.builder()
                .user(user)
                .title(r.title())
                .companyName(r.company())
                .location(r.location())
                .description(r.description())
                .employmentType(normalizeEmploymentType(r.employmentType()))
                .seniorityLevel(r.seniorityLevel())
                .requiredSkills(new ArrayList<>())
                .preferredSkills(new ArrayList<>())
                .technologies(new ArrayList<>())
                .salaryMin(salary.min())
                .salaryMax(salary.max())
                .currency(salary.currency())
                .applicationUrl(r.url())
                .companyLogoUrl(r.companyLogo())
                .postedLabel(r.postedLabel())
                .applicantsLabel(r.applicants())
                .sourceUrl(sourceUrl)
                .sourceType(JobSourceType.URL)
                .build();
    }
    private static String normalizeEmploymentType(String raw) {
        if (raw == null) return null;
        String upper = raw.toUpperCase().replace(" ", "_");
        return switch (upper) {
            case "FULL_TIME", "FULLTIME" -> "FULL_TIME";
            case "PART_TIME", "PARTTIME" -> "PART_TIME";
            case "CONTRACT", "CONTRACTOR" -> "CONTRACT";
            case "INTERNSHIP", "INTERN" -> "INTERNSHIP";
            default -> raw;
        };
    }
    private static final java.util.regex.Pattern SALARY_PATTERN = java.util.regex.Pattern.compile(
            "([€$£])?\\s?([\\d,]+)(?:\\s*-\\s*([€$£])?\\s?([\\d,]+))?");

    private record ParsedSalary(Integer min, Integer max, String currency) {}

    private static ParsedSalary parseSalary(Object raw) {
        if (!(raw instanceof String s) || s.isBlank()) return new ParsedSalary(null, null, null);
        var m = SALARY_PATTERN.matcher(s);
        if (!m.find()) return new ParsedSalary(null, null, null);

        Integer min = parseAmount(m.group(2));
        Integer max = m.group(4) != null ? parseAmount(m.group(4)) : min;
        String currency = symbolToCurrency(m.group(1) != null ? m.group(1) : m.group(3));
        return new ParsedSalary(min, max, currency);
    }

    private static Integer parseAmount(String digits) {
        if (digits == null) return null;
        try { return Integer.parseInt(digits.replace(",", "")); }
        catch (NumberFormatException e) { return null; }
    }

    private static String symbolToCurrency(String symbol) {
        if (symbol == null) return null;
        return switch (symbol) {
            case "$" -> "USD";
            case "€" -> "EUR";
            case "£" -> "GBP";
            default -> null;
        };
    }
}
