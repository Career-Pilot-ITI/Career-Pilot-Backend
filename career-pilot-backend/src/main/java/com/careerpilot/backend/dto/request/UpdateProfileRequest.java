package com.careerpilot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateProfileRequest {
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username must be alphanumeric with underscores only")
    @Schema(example = "amr_shams")
    private String username;

    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "Invalid email format")
    @Schema(example = "amr.shams@example.com")
    private String email;

    @Schema(example = "currentPass123", description = "Required when setting a new password")
    private String currentPassword;

    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(example = "newPass456")
    private String newPassword;

    @Schema(example = "Amr Shams")
    private String displayName;

    @Schema(example = "7", description = "ID of an uploaded avatar file (from /api/v1/files/upload)")
    private Long avatarFileId;

    @Pattern(regexp = "^(male|female)$", message = "Gender must be male or female")
    @Schema(example = "male")
    private String gender;

    @Schema(example = "1998-05-15")
    private LocalDate dateOfBirth;

    @Schema(example = "Senior Backend Engineer")
    private String targetRole;

    @Schema(example = "Technology")
    private String industry;

    @Schema(example = "senior")
    private String experienceLevel;

    @Schema(example = "Backend Engineer at Aramco")
    private String currentJobTitle;

    @Schema(example = "5")
    private Integer yearsOfExperience;

    @Schema(example = "42", description = "ID of an uploaded CV file (from /api/v1/files/upload)")
    private Long cvFileId;

    @Schema(example = "[\"Java\",\"Spring Boot\",\"PostgreSQL\"]")
    private List<String> skills;

    @Schema(example = "[\"Aramco\",\"STC\"]")
    private List<String> targetCompanies;

    @Schema(example = "bachelors")
    private String educationLevel;

    @Schema(example = "Africa/Cairo")
    private String timezone;

    @Schema(example = "true")
    private Boolean termsAccepted;

    @Schema(example = "true")
    private Boolean onboardingCompleted;

    @Schema(example = "PLUS")
    private String subscriptionTier;

    @Schema(example = "1")
    private Long trackId;
}
