package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.UserProfile;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class UserProfileResponse {
    private String displayName;
    private String username;
    private String email;
    private String avatarUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String targetRole;
    private String industry;
    private String experienceLevel;
    private String currentJobTitle;
    private Integer yearsOfExperience;
    private String resumeUrl;
    private List<String> skills;
    private List<String> targetCompanies;
    private String educationLevel;
    private String timezone;
    private Boolean termsAccepted;

    public static UserProfileResponse from(UserProfile profile) {
        UserProfileResponse r = new UserProfileResponse();

        if (profile.getUser() != null) {
            r.setUsername(profile.getUser().getUsername());
            r.setEmail(profile.getUser().getEmail());
        }

        r.setDisplayName(profile.getDisplayName());
        r.setAvatarUrl(profile.getAvatarUrl());
        r.setGender(profile.getGender());
        r.setDateOfBirth(profile.getDateOfBirth());
        r.setTargetRole(profile.getTargetRole());
        r.setIndustry(profile.getIndustry());
        r.setExperienceLevel(profile.getExperienceLevel());
        r.setCurrentJobTitle(profile.getCurrentJobTitle());
        r.setYearsOfExperience(profile.getYearsOfExperience());
        r.setResumeUrl(profile.getCvUrl());
        r.setEducationLevel(profile.getEducationLevel());
        r.setTimezone(profile.getTimezone());
        r.setTermsAccepted(profile.getTermsAccepted());

        return r;
    }
}
