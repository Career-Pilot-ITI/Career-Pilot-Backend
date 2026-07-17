package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.response.UserFileResponse;
import com.careerpilot.backend.controller.response.UserProfileResponse;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import com.careerpilot.backend.dto.response.CvAnalysis;


import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.entity.UserFile;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.entity.ENUMs.SkillCategory;
import com.careerpilot.backend.repository.IUserFileRepository;
import com.careerpilot.backend.repository.IUserProfileRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.repository.IUserSkillRepository;
import com.careerpilot.backend.service.ICvExtractionService;
import com.careerpilot.backend.service.IFileUploadService;
import com.careerpilot.backend.service.ILlmService;
import com.careerpilot.backend.service.IUserProfileService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileServiceImpl implements IUserProfileService {

  private final IUserRepository userRepository;
  private final IUserProfileRepository profileRepository;
  private final IUserSkillRepository skillRepository;
  private final IUserFileRepository fileRepository;
  private final IFileUploadService fileUploadService;
  private final ICvExtractionService cvExtractionService;
  private final ILlmService llmService;
  private final EntityManager entityManager;

  @Value("${app.upload.path:./uploads}")
  private String uploadPath;

  @Override
  @Transactional
  public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RuntimeException("User not found"));

    if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
      if (userRepository.findByUsername(request.getUsername()).isPresent()) {
        throw new RuntimeException("Username already taken");
      }
      user.setUsername(request.getUsername());
    }

    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.findByEmail(request.getEmail()).isPresent()) {
        throw new RuntimeException("Email already registered");
      }
      user.setEmail(request.getEmail());
    }

    user.setUpdatedAt(LocalDateTime.now());
    userRepository.save(user);

    UserProfile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("User profile not found"));

    if (request.getDisplayName() != null)
      profile.setDisplayName(request.getDisplayName());
    if (request.getGender() != null)
      profile.setGender(request.getGender());
    if (request.getDateOfBirth() != null)
      profile.setDateOfBirth(request.getDateOfBirth());
    if (request.getTargetRole() != null)
      profile.setTargetRole(request.getTargetRole());
    if (request.getIndustry() != null)
      profile.setIndustry(request.getIndustry());
    if (request.getExperienceLevel() != null)
      profile.setExperienceLevel(request.getExperienceLevel());
    if (request.getCurrentJobTitle() != null)
      profile.setCurrentJobTitle(request.getCurrentJobTitle());
    if (request.getYearsOfExperience() != null)
      profile.setYearsOfExperience(request.getYearsOfExperience());

    if (request.getAvatarFileId() != null) {
      UserFile avatarFile = fileRepository.findByIdAndUserId(request.getAvatarFileId(), userId)
          .orElseThrow(() -> new RuntimeException("Avatar file not found or does not belong to you"));
      profile.setAvatarUrl(avatarFile.getStoredPath());
    }
    if (request.getCvFileId() != null) {
      UserFile cvFile = fileRepository.findByIdAndUserId(request.getCvFileId(), userId)
          .orElseThrow(() -> new RuntimeException("CV file not found or does not belong to you"));
      profile.setCvUrl(cvFile.getStoredPath());
    }
    if (request.getTargetCompanies() != null)
      profile.setTargetCompanies(String.join(",", request.getTargetCompanies()));
    if (request.getEducationLevel() != null)
      profile.setEducationLevel(request.getEducationLevel());
    if (request.getTimezone() != null)
      profile.setTimezone(request.getTimezone());
    if (request.getTermsAccepted() != null)
      profile.setTermsAccepted(request.getTermsAccepted());
    if (request.getSubscriptionTier() != null)
      profile.setSubscriptionTier(request.getSubscriptionTier());
    if (request.getOnboardingCompleted() != null)
      profile.setOnboardingCompleted(request.getOnboardingCompleted());
    if (request.getTrackId() != null) {
      var track = entityManager.find(com.careerpilot.backend.entity.Track.class, request.getTrackId());
      if (track != null)
        profile.setTrack(track);
    }
    profile.setUpdatedAt(LocalDateTime.now());

    profileRepository.save(profile);

    if (request.getSkills() != null) {
      saveSkills(userId, request.getSkills());
    }

    return buildProfileResponse(profile, userId);
  }

  @Override
  @Transactional(readOnly = true)
  public UserProfileResponse getProfile(Long userId) {
    UserProfile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("User profile not found"));
    return buildProfileResponse(profile, userId);
  }

  @Override
  @Transactional
  public UserProfileResponse analyzeCv(Long userId, MultipartFile file) {
    UserFileResponse fileResponse = fileUploadService.upload(file, "cvs", userId);

    UserFile userFile = fileRepository.findById(fileResponse.getId())
        .orElseThrow(() -> new RuntimeException("File not found after upload"));
    String storedPath = userFile.getStoredPath();
    String relativePath = storedPath.replace("/api/v1/files/", "");
    Path absolutePath = Paths.get(uploadPath).toAbsolutePath().normalize().resolve(relativePath);

    String cvText;
    try {
      cvText = cvExtractionService.extractCv(absolutePath);
    } catch (IOException e) {
      log.error("Tika extraction failed for user {}", userId, e);
      throw new RuntimeException("Failed to extract text from CV file", e);
    }
    if (cvText.isBlank()) {
      throw new RuntimeException("Could not extract text from CV. The file may be a scanned image. Please upload a text-based PDF or DOCX.");
    }

    CvAnalysis analysis = llmService.analyzeCv(cvText);

    UserProfile profile = profileRepository.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("User profile not found"));
    profile.setCvUrl(storedPath);
    profile.setCvFilename(file.getOriginalFilename());
    profile.setCvUploadedAt(LocalDateTime.now());
    if (analysis.targetRole() != null && !analysis.targetRole().isBlank())
      profile.setTargetRole(analysis.targetRole());
    if (analysis.yearsOfExperience() > 0)
      profile.setYearsOfExperience(analysis.yearsOfExperience());
    if (analysis.educationLevel() != null && !analysis.educationLevel().isBlank())
      profile.setEducationLevel(analysis.educationLevel());
    if (analysis.displayName() != null && !analysis.displayName().isBlank())
      profile.setDisplayName(analysis.displayName());
    if (analysis.currentJobTitle() != null && !analysis.currentJobTitle().isBlank())
      profile.setCurrentJobTitle(analysis.currentJobTitle());
    profile.setUpdatedAt(LocalDateTime.now());
    profileRepository.save(profile);

    if (analysis.skills() != null && !analysis.skills().isEmpty()) {
      saveSkills(userId, analysis.skills());
    }

    return buildProfileResponse(profile, userId);
  }

  private void saveSkills(Long userId, List<String> skillNames) {
    skillRepository.deleteByUserId(userId);
    for (String skillName : skillNames) {
      if (skillName == null || skillName.isBlank())
        continue;
      UserSkill skill = new UserSkill();
      skill.setUser(userRepository.getReferenceById(userId));
      skill.setSkillName(skillName.trim());
      skill.setCategory(SkillCategory.TECHNICAL);
      skill.setCreatedAt(LocalDateTime.now());
      skillRepository.save(skill);
    }
  }

  private UserProfileResponse buildProfileResponse(UserProfile profile, Long userId) {
    UserProfileResponse response = UserProfileResponse.from(profile);
    response.setSkills(getSkillNames(userId));
    return response;
  }

  private List<String> getSkillNames(Long userId) {
    List<UserSkill> userSkills = skillRepository.findByUserId(userId);
    if (userSkills == null || userSkills.isEmpty()) {
      return Collections.emptyList();
    }
    return userSkills.stream()
        .map(UserSkill::getSkillName)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void createDefaultProfile(Long userId) {
    User user = userRepository.getReferenceById(userId);
    UserProfile profile = profileRepository.findByUserId(userId).orElse(null);
    if (profile != null)
      return; // already exists

    profile = new UserProfile();
    profile.setUser(user);
    profileRepository.save(profile);
  }
}
