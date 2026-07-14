package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CompleteRegistrationRequest {
    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric with underscores only")
    private String username;

    @Pattern(regexp = "^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$", message = "Invalid email format")
    private String email;

    private String avatarUrl;

    @Pattern(regexp = "^(male|female)$", message = "Gender must be male or female")
    private String gender;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Target role is required")
    private String targetRole;

    @NotBlank(message = "Industry is required")
    private String industry;

    @NotBlank(message = "Experience level is required")
    @Pattern(regexp = "^(student|entry|mid|senior|executive)$", message = "Invalid experience level")
    private String experienceLevel;

    private String currentJobTitle;

    private Integer yearsOfExperience;

    private String resumeUrl;

    private List<String> skills;

    private List<String> targetCompanies;

    private String educationLevel;

    private Boolean termsAccepted;
}
