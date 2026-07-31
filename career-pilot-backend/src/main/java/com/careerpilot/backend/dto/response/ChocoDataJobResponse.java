package com.careerpilot.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Parsed LinkedIn job posting returned by the ChocoData API
 * (GET /api/v1/linkedin/job). Unknown fields in the response are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChocoDataJobResponse(
        @JsonProperty("job_id") String jobId,
        String url,
        String title,
        String company,
        @JsonProperty("company_url") String companyUrl,
        String location,
        @JsonProperty("employment_type") String employmentType,
        @JsonProperty("seniority_level") String seniorityLevel,
        @JsonProperty("job_function") String jobFunction,
        String industries,
        Object salary,
        @JsonProperty("date_posted") String datePosted,
        String description,
        @JsonProperty("description_html") String descriptionHtml
) {
}
