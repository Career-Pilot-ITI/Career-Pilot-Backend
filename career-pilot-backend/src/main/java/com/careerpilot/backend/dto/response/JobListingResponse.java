package com.careerpilot.backend.dto.response;

import com.careerpilot.backend.entity.JobListing;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobListingResponse {
  private Long id;
  private String title;
  private String companyName;
  private String location;
  private String description;
  private String employmentType;
  private String seniorityLevel;
  private List<String> requiredSkills;
  private List<String> preferredSkills;
  private String responsibilities;
  private String qualifications;
  private List<String> technologies;
  private Integer salaryMin;
  private Integer salaryMax;
  private String currency;
  private Integer experienceYears;
  private String educationLevel;
  private String applicationUrl;
  private String sourceUrl;
  private String sourceType;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String companyLogoUrl;
  private String postedLabel;
  private String applicantsLabel;

  public static JobListingResponse from(JobListing j) {
    if (j == null)
      return null;
    return JobListingResponse.builder()
        .id(j.getId())
        .title(j.getTitle())
        .companyName(j.getCompanyName())
        .location(j.getLocation())
        .description(j.getDescription())
        .employmentType(j.getEmploymentType())
        .seniorityLevel(j.getSeniorityLevel())
        .requiredSkills(j.getRequiredSkills())
        .preferredSkills(j.getPreferredSkills())
        .responsibilities(j.getResponsibilities())
        .qualifications(j.getQualifications())
        .technologies(j.getTechnologies())
        .salaryMin(j.getSalaryMin())
        .salaryMax(j.getSalaryMax())
        .currency(j.getCurrency())
        .experienceYears(j.getExperienceYears())
        .educationLevel(j.getEducationLevel())
        .applicationUrl(j.getApplicationUrl())
        .sourceUrl(j.getSourceUrl())
        .sourceType(j.getSourceType() != null ? j.getSourceType().name() : null)
        .createdAt(j.getCreatedAt())
        .updatedAt(j.getUpdatedAt())
        .companyLogoUrl(j.getCompanyLogoUrl())
        .postedLabel(j.getPostedLabel())
        .applicantsLabel(j.getApplicantsLabel())
        .build();
  }
}
