package com.careerpilot.backend.controller;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.controller.response.LoginResponse;
import com.careerpilot.backend.controller.response.OtpAuthResponse;
import com.careerpilot.backend.dto.LoginUserDto;
import com.careerpilot.backend.dto.RegisterUserDto;
import com.careerpilot.backend.dto.request.CompleteRegistrationRequest;
import com.careerpilot.backend.dto.request.RefreshTokenRequest;
import com.careerpilot.backend.dto.request.SendOtpRequest;
import com.careerpilot.backend.dto.request.VerifyOtpRequest;
import com.careerpilot.backend.dto.request.VerifyRequest;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IAuthentication;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthentication iAuthentication;

    @Value("${app.redirect.uri.app}")
    private String redirectUriBase;

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginUserDto loginUserDto) {
        LoginResponse loginResponse = iAuthentication.login(loginUserDto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginResponse);
    }

    @GetMapping("/oauth-login")
    public void oauthLogin(OAuth2AuthenticationToken authentication, HttpServletResponse response) throws IOException {
        try {
            String token = iAuthentication.handleOAuthLogin(authentication);
            response.sendRedirect(redirectUriBase + "?token=" + token);
        } catch (IllegalArgumentException e) {
            response.sendRedirect(redirectUriBase + "?error=" + e.getMessage());
        } catch (Exception e) {
            response.sendRedirect(redirectUriBase + "?error=OAuth login failed");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody RegisterUserDto registerUserDto) {
        iAuthentication.signup(registerUserDto);
        ApiResponse apiResponse = new ApiResponse("Code sent");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PutMapping("/verify")
    public ResponseEntity<ApiResponse> verifyUser(@Valid @RequestBody VerifyRequest request) {
        iAuthentication.verifyUser(request.getEmail(), request.getVerificationCode());
        ApiResponse apiResponse = new ApiResponse("Account verified successfully");
        return ResponseEntity.ok().body(apiResponse);
    }

    @GetMapping("/resend-code")
    public ResponseEntity<ApiResponse> resendVerificationCode(@RequestParam String email) {
        iAuthentication.resendVerificationCode(email);
        ApiResponse apiResponse = new ApiResponse("Verification code resent");
        return ResponseEntity.ok().body(apiResponse);
    }

    @GetMapping("/reset-password-request")
    public ResponseEntity<ApiResponse> requestPasswordReset(@RequestParam String email) {
        iAuthentication.requestPasswordReset(email);
        ApiResponse apiResponse = new ApiResponse("Password reset email sent");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PutMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestParam String email, @RequestParam String verificationCode, @RequestParam String newPassword) {
        iAuthentication.resetPassword(email, verificationCode, newPassword);
        ApiResponse apiResponse = new ApiResponse("Password reset successful");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse loginResponse = iAuthentication.refresh(request.getRefreshToken());
        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse("Invalid token"));
        }
        String token = authorizationHeader.substring(7);
        iAuthentication.logout(token);
        return ResponseEntity.ok(new ApiResponse("Logout successful"));
    }
    @PostMapping("/send-otp")
    @RateLimit(capacity = 3, refillTokens = 3, refillSeconds = 60, key = "#request.phoneNumber")
    public ResponseEntity<ApiResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        iAuthentication.sendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(new ApiResponse("OTP sent"));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<OtpAuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpAuthResponse response = iAuthentication.loginWithOtp(request.getPhoneNumber(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<ApiResponse> completeRegistration(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CompleteRegistrationRequest request) {
        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;
        iAuthentication.completeRegistration(customUserDetails.getUser().getId(), request);
        return ResponseEntity.ok(new ApiResponse("Registration completed"));
    }
}
