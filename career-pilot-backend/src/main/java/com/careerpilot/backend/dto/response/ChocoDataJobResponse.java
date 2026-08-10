package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChocoDataJobResponse(
        @JsonProperty("job_id") String jobId,
        String url,
        String title,
        String company,
        @JsonProperty("company_url") String companyUrl,
        String location,
        @JsonProperty("employment_type") String employmentType,
        @JsonProperty("seniority") String seniorityLevel,
        @JsonProperty("job_function") String jobFunction,
        String industries,
        Object salary,
        @JsonProperty("posted_label") String postedLabel,
        String description,
        @JsonProperty("description_html") String descriptionHtml,
        @JsonProperty("company_logo") String companyLogo,
        @JsonProperty("applicants") String applicants
) {
}