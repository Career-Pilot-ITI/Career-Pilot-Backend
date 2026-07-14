package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.AuthException;
import com.careerpilot.backend.controller.response.AuthTokensResponse;
import com.careerpilot.backend.controller.response.LoginResponse;
import com.careerpilot.backend.controller.response.OtpAuthResponse;
import com.careerpilot.backend.controller.response.UserResponse;
import com.careerpilot.backend.dto.LoginUserDto;
import com.careerpilot.backend.dto.RegisterUserDto;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import com.careerpilot.backend.dto.request.VerifyOtpRequest;
import com.careerpilot.backend.entity.Role;
import com.careerpilot.backend.entity.Track;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.entity.UserProfile;
import com.careerpilot.backend.entity.UserRole;
import com.careerpilot.backend.entity.UserSkill;
import com.careerpilot.backend.entity.ENUMs.SkillCategory;
import com.careerpilot.backend.repository.IRoleRepository;
import com.careerpilot.backend.repository.IUserProfileRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.repository.IUserSkillRepository;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.security.jwt.JwtService;
import com.careerpilot.backend.security.jwt.RefreshTokenService;
import com.careerpilot.backend.security.jwt.TokenBlacklistService;
import com.careerpilot.backend.service.IAuthentication;
import com.careerpilot.backend.service.IOtpService;
import com.careerpilot.backend.utils.IEmailService;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements IAuthentication {

  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;
  private final IUserRepository iUserRepository;
  private final IRoleRepository iRoleRepository;
  private final PasswordEncoder passwordEncoder;
  private final IEmailService emailService;
  private final TokenBlacklistService tokenBlacklistService;
  private final RefreshTokenService refreshTokenService;
  private final IOtpService otpService;
  private final IUserProfileRepository iUserProfileRepository;
  private final IUserSkillRepository iUserSkillRepository;
  private final EntityManager entityManager;
  private User user;

  @Override
  public LoginResponse login(LoginUserDto loginUserDto) {
    validateLoginCredentials(loginUserDto);

    Authentication authentication = authenticateUser(loginUserDto);
    CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
    verifyAccountStatus(customUserDetails);

    String accessToken = jwtService.generateToken(customUserDetails);
    String refreshToken = refreshTokenService.generateRefreshToken(customUserDetails.getUsername());
    return new LoginResponse(accessToken, jwtService.getExpirationTime(), refreshToken);
  }

  @Override
  public void sendOtp(String phoneNumber) {
    otpService.sendPhoneOtp(phoneNumber);
  }

  @Override
  public OtpAuthResponse loginWithOtp(String phoneNumber, String code) {
    otpService.verifyPhoneOtp(phoneNumber, code);

    boolean isNewUser = false;
    User user = iUserRepository.findByPhoneNumber(phoneNumber).orElse(null);
    if (user == null) {
      isNewUser = true;
      user = new User();
      user.setPhoneNumber(phoneNumber);
      user.setUsername("user_" + phoneNumber.substring(Math.max(0, phoneNumber.length() - 6)));
      user.setEnabled(true);
      user.setCreatedAt(LocalDateTime.now());

      Role role = iRoleRepository.findByName("ROLE_USER")
          .orElseThrow(() -> new RuntimeException("User role not found"));

      UserRole userRole = new UserRole();
      userRole.setRole(role);
      userRole.setUser(user);
      userRole.setStatus(true);

      user.getUserRoles().add(userRole);
      iUserRepository.save(user);
    }

    UserProfile profile = iUserProfileRepository.findByUserId(user.getId()).orElse(null);
    if (profile == null) {
      profile = new UserProfile();
      profile.setUser(user);
      profile.setCreatedAt(LocalDateTime.now());
      iUserProfileRepository.save(profile);
    }

    CustomUserDetails userDetails = new CustomUserDetails(user);
    String accessToken = jwtService.generateToken(userDetails);
    String refreshToken = refreshTokenService.generateRefreshToken(user.getUsername());

    AuthTokensResponse tokens = new AuthTokensResponse(accessToken, refreshToken, jwtService.getExpirationTime());
    UserResponse userResponse = UserResponse.from(user, profile, isNewUser);
    userResponse.getProfile().setSkills(getSkillNames(user.getId()));
    return new OtpAuthResponse(tokens, userResponse);
  }

  @Override
  public OtpAuthResponse verifyOtp(VerifyOtpRequest request) {
    return loginWithOtp(request.getPhoneNumber(), request.getCode());
  }

  @Override
  @Transactional
  public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
    User user = iUserRepository.findById(userId)
        .orElseThrow(() -> new AuthException.UserNotFoundException("User not found"));

    if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
      if (iUserRepository.findByUsername(request.getUsername()).isPresent()) {
        throw new AuthException.UsernameAlreadyExistsException("Username already taken");
      }
      user.setUsername(request.getUsername());
    }

    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (iUserRepository.findByEmail(request.getEmail()).isPresent()) {
        throw new AuthException.UserAlreadyExistsException("Email already registered");
      }
      user.setEmail(request.getEmail());
    }

    if (request.getNewPassword() != null) {
      if (user.getPassword() != null) {
        if (request.getCurrentPassword() == null
            || !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
          throw new AuthException.UserNotFoundException("Current password is incorrect");
        }
      }
      user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    user.setUpdatedAt(LocalDateTime.now());
    iUserRepository.save(user);

    UserProfile profile = iUserProfileRepository.findByUserId(userId)
        .orElseThrow(() -> new RuntimeException("User profile not found"));

    if (request.getDisplayName() != null) profile.setDisplayName(request.getDisplayName());
    if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
    if (request.getGender() != null) profile.setGender(request.getGender());
    if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
    if (request.getTargetRole() != null) profile.setTargetRole(request.getTargetRole());
    if (request.getIndustry() != null) profile.setIndustry(request.getIndustry());
    if (request.getExperienceLevel() != null) profile.setExperienceLevel(request.getExperienceLevel());
    if (request.getCurrentJobTitle() != null) profile.setCurrentJobTitle(request.getCurrentJobTitle());
    if (request.getYearsOfExperience() != null) profile.setYearsOfExperience(request.getYearsOfExperience());
    if (request.getCvUrl() != null) profile.setCvUrl(request.getCvUrl());
    if (request.getTargetCompanies() != null)
      profile.setTargetCompanies(String.join(",", request.getTargetCompanies()));
    if (request.getEducationLevel() != null) profile.setEducationLevel(request.getEducationLevel());
    if (request.getTimezone() != null) profile.setTimezone(request.getTimezone());
    if (request.getTermsAccepted() != null) profile.setTermsAccepted(request.getTermsAccepted());
    if (request.getSubscriptionTier() != null) profile.setSubscriptionTier(request.getSubscriptionTier());
    if (request.getTrackId() != null) {
      Track track = entityManager.find(Track.class, request.getTrackId());
      if (track != null) profile.setTrack(track);
    }
    profile.setUpdatedAt(LocalDateTime.now());

    iUserProfileRepository.save(profile);

    if (request.getSkills() != null) {
      iUserSkillRepository.deleteByUserId(userId);
      for (String skillName : request.getSkills()) {
        if (skillName == null || skillName.isBlank()) continue;
        UserSkill skill = new UserSkill();
        skill.setUser(user);
        skill.setSkillName(skillName.trim());
        skill.setCategory(SkillCategory.TECHNICAL);
        skill.setCreatedAt(LocalDateTime.now());
        iUserSkillRepository.save(skill);
      }
    }

    UserResponse userResponse = UserResponse.from(user, profile, false);
    userResponse.getProfile().setSkills(getSkillNames(userId));
    return userResponse;
  }

  @Override
  public LoginResponse refresh(String refreshToken) {
    String username = refreshTokenService.validateAndRotate(refreshToken)
        .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));

    User user = iUserRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));

    CustomUserDetails userDetails = new CustomUserDetails(user);
    String newAccessToken = jwtService.generateToken(userDetails);
    String newRefreshToken = refreshTokenService.generateRefreshToken(username);
    return new LoginResponse(newAccessToken, jwtService.getExpirationTime(), newRefreshToken);
  }

  @Override
  public void logout(String token) {
    long remainingTtl = jwtService.extractAllClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    tokenBlacklistService.blacklistToken(token, Math.max(remainingTtl, 0));

    String username = jwtService.extractUsername(token);
    refreshTokenService.revokeAllForUser(username);

    SecurityContextHolder.clearContext();
  }

  /** @deprecated Use OTP signup instead */
  @Override
  @Deprecated
  public User signup(RegisterUserDto registerUserDto) {
    checkIfUserExists(registerUserDto);

    User newUser = createNewUser(registerUserDto);
    String verificationCode = generateVerificationCode();
    newUser.setVerificationCode(verificationCode);
    newUser.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));

    iUserRepository.save(newUser);
    sendVerificationEmail(newUser);

    return newUser;
  }

  @Override
  @Deprecated
  public void verifyUser(String email, String verificationCode) {
    User user = getUserByEmailOrThrow(email);
    validateVerificationCode(user, verificationCode);
    enableUserAccount(user);
  }

  @Override
  @Deprecated
  public void resendVerificationCode(String email) {
    User user = getUserByEmailOrThrow(email);
    if (user.isEnabled()) {
      throw new RuntimeException("Account is already verified");
    }
    String verificationCode = generateVerificationCode();
    user.setVerificationCode(verificationCode);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusHours(1));
    sendVerificationEmail(user);
    iUserRepository.save(user);
  }

  @Override
  @Deprecated
  public void requestPasswordReset(String email) {
    User user = getUserByEmailOrThrow(email);
    String verificationCode = generateVerificationCode();
    user.setVerificationCode(verificationCode);
    user.setVerificationCodeExpiresAt(LocalDateTime.now().plusMinutes(15));
    sendVerificationEmail(user);
    iUserRepository.save(user);
  }

  @Override
  @Deprecated
  public void resetPassword(String email, String verificationCode, String newPassword) {
    User user = getUserByEmailOrThrow(email);
    validateVerificationCode(user, verificationCode);
    user.setPassword(passwordEncoder.encode(newPassword));
    user.setVerificationCode(null);
    user.setVerificationCodeExpiresAt(null);
    iUserRepository.save(user);
  }

  @Override
  public String handleOAuthLogin(OAuth2AuthenticationToken authentication) {
    if (authentication == null || authentication.getPrincipal() == null) {
      throw new IllegalArgumentException("Authentication token or principal is null");
    }

    OAuth2User oAuth2User = authentication.getPrincipal();
    String provider = authentication.getAuthorizedClientRegistrationId();
    String email = oAuth2User.getAttribute("email");
    String name = getOAuthProviderName(oAuth2User, provider);

    User user = iUserRepository.findByEmail(email).orElseGet(() -> createOAuthUser(name, email));
    CustomUserDetails userDetails = new CustomUserDetails(user);
    return jwtService.generateToken(userDetails);
  }

  private void validateLoginCredentials(LoginUserDto loginUserDto) {
    if (loginUserDto == null || loginUserDto.getIdentifier() == null || loginUserDto.getPassword() == null) {
      throw new AuthException.UserNotFoundException("Username and password must not be null");
    }
  }

  private Authentication authenticateUser(LoginUserDto loginUserDto) {
    try {
      return authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(loginUserDto.getIdentifier(), loginUserDto.getPassword()));
    } catch (Exception e) {
      throw new AuthException.UserNotFoundException("Invalid username or password");
    }
  }

  private void verifyAccountStatus(CustomUserDetails customUserDetails) {
    if (!customUserDetails.getUser().isEnabled()) {
      throw new AuthException.AccountNotVerifiedException("Account not verified. Please verify your account.");
    }
  }

  private void checkIfUserExists(RegisterUserDto registerUserDto) {
    if (registerUserDto.getEmail() != null && !registerUserDto.getEmail().isBlank()
        && iUserRepository.findByEmail(registerUserDto.getEmail()).isPresent()) {
      throw new AuthException.UserAlreadyExistsException("Email already registered");
    }

    if (iUserRepository.findByUsername(registerUserDto.getUsername()).isPresent()) {
      throw new AuthException.UsernameAlreadyExistsException("Username already taken");
    }

    if (iUserRepository.findByPhoneNumber(registerUserDto.getPhoneNumber()).isPresent()) {
      throw new AuthException.UserAlreadyExistsException("Phone number already registered");
    }
  }

  private User createNewUser(RegisterUserDto registerUserDto) {
    User newUser = new User();
    newUser.setEmail(registerUserDto.getEmail());
    newUser.setUsername(registerUserDto.getUsername());
    newUser.setPhoneNumber(registerUserDto.getPhoneNumber());
    newUser.setPassword(passwordEncoder.encode(registerUserDto.getPassword()));
    newUser.setEnabled(false);

    Role role = iRoleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("User role not found"));

    UserRole userRole = new UserRole();
    userRole.setRole(role);
    userRole.setUser(newUser);
    userRole.setStatus(true);

    newUser.getUserRoles().add(userRole);
    return newUser;
  }

  private void sendVerificationEmail(User user) {
    String subject = "Please verify your email";
    String htmlMessage = "<html><body>Please use the following code to verify your email: " + user.getVerificationCode()
        + "</body></html>";
    try {
      emailService.sendVerificationEmail(user.getEmail(), subject, htmlMessage);
    } catch (MessagingException e) {
      throw new RuntimeException("Error sending verification email.");
    }
  }

  private String generateVerificationCode() {
    Random random = new Random();
    return String.valueOf(random.nextInt(900000) + 100000);
  }

  private User getUserByEmailOrThrow(String email) {
    return iUserRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("User not found"));
  }

  private void validateVerificationCode(User user, String verificationCode) {
    if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
      throw new RuntimeException("Verification code has expired");
    }
    if (!user.getVerificationCode().equals(verificationCode)) {
      throw new RuntimeException("Invalid verification code");
    }
  }

  private void enableUserAccount(User user) {
    user.setEnabled(true);
    user.setVerificationCode(null);
    user.setVerificationCodeExpiresAt(null);
    iUserRepository.save(user);
  }

  private String getOAuthProviderName(OAuth2User oAuth2User, String provider) {
    String fullName;
    if ("google".equals(provider)) {
      fullName = oAuth2User.getAttribute("name");
    } else if ("github".equals(provider)) {
      fullName = oAuth2User.getAttribute("login");
    } else {
      throw new IllegalArgumentException("Unsupported provider: " + provider);
    }

    if (fullName != null && fullName.contains(" ")) {
      return fullName.split(" ")[0];
    }
    return fullName;
  }

  private List<String> getSkillNames(Long userId) {
    List<UserSkill> userSkills = iUserSkillRepository.findByUserId(userId);
    if (userSkills == null || userSkills.isEmpty()) {
      return Collections.emptyList();
    }
    return userSkills.stream()
        .map(UserSkill::getSkillName)
        .collect(Collectors.toList());
  }

  private User createOAuthUser(String name, String email) {
    User newUser = new User();
    newUser.setEmail(email);
    newUser.setUsername(name);
    newUser.setPassword(null);
    newUser.setEnabled(true);

    Role role = iRoleRepository.findByName("ROLE_USER")
        .orElseThrow(() -> new RuntimeException("User role not found"));

    UserRole userRole = new UserRole();
    userRole.setRole(role);
    userRole.setUser(newUser);
    userRole.setStatus(true);

    newUser.getUserRoles().add(userRole);
    return iUserRepository.save(newUser);
  }
}
