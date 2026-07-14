package com.careerpilot.backend.service;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import com.careerpilot.backend.controller.response.LoginResponse;
import com.careerpilot.backend.controller.response.OtpAuthResponse;
import com.careerpilot.backend.controller.response.UserResponse;
import com.careerpilot.backend.dto.LoginUserDto;
import com.careerpilot.backend.dto.RegisterUserDto;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import com.careerpilot.backend.dto.request.VerifyOtpRequest;
import com.careerpilot.backend.entity.User;

public interface IAuthentication {
  LoginResponse login(LoginUserDto loginUserDto);

  void sendOtp(String phoneNumber);

  OtpAuthResponse loginWithOtp(String phoneNumber, String code);

  OtpAuthResponse verifyOtp(VerifyOtpRequest request);

  LoginResponse refresh(String refreshToken);

  void logout(String token);

  UserResponse updateProfile(Long userId, UpdateProfileRequest request);

  /** @deprecated Use OTP signup instead */
  @Deprecated
  User signup(RegisterUserDto registerUserDto);

  /** @deprecated OTP-based auth replaces email verification */
  @Deprecated
  void verifyUser(String email, String verificationCode);

  /** @deprecated OTP-based auth replaces email verification */
  @Deprecated
  void resendVerificationCode(String email);

  /** @deprecated OTP-based auth replaces email verification */
  @Deprecated
  void requestPasswordReset(String email);

  /** @deprecated OTP-based auth replaces email verification */
  @Deprecated
  void resetPassword(String email, String verificationCode, String newPassword);

  String handleOAuthLogin(OAuth2AuthenticationToken authentication);
}
