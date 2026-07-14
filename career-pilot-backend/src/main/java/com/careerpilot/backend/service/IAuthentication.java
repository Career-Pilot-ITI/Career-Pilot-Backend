package com.careerpilot.backend.service;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import com.careerpilot.backend.controller.response.LoginResponse;
import com.careerpilot.backend.controller.response.OtpAuthResponse;
import com.careerpilot.backend.dto.LoginUserDto;
import com.careerpilot.backend.dto.RegisterUserDto;
import com.careerpilot.backend.dto.request.CompleteRegistrationRequest;
import com.careerpilot.backend.entity.User;

public interface IAuthentication {
  LoginResponse login(LoginUserDto loginUserDto);

  void sendOtp(String phoneNumber);

  OtpAuthResponse loginWithOtp(String phoneNumber, String code);

  LoginResponse refresh(String refreshToken);

  void logout(String token);

  User signup(RegisterUserDto registerUserDto);

  void verifyUser(String email, String verificationCode);

  void resendVerificationCode(String email);

  void requestPasswordReset(String email);

  void resetPassword(String email, String verificationCode, String newPassword);

  String handleOAuthLogin(OAuth2AuthenticationToken authentication);

  void completeRegistration(Long userId, CompleteRegistrationRequest request);
}
