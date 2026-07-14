package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.controller.response.LoginResponse;
import com.careerpilot.backend.controller.response.UserResponse;
import com.careerpilot.backend.dto.LoginUserDto;
import com.careerpilot.backend.dto.RegisterUserDto;
import com.careerpilot.backend.dto.request.RefreshTokenRequest;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import com.careerpilot.backend.dto.request.VerifyRequest;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Email/password authentication, OAuth, and token management")
public class AuthController {

    private final IAuthentication iAuthentication;

    @Value("${app.redirect.uri.app}")
    private String redirectUriBase;

    @GetMapping("/hello")
    @Operation(summary = "Health check", description = "Returns a simple greeting to verify the service is running")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello");
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email or username", description = "Authenticate using email/username and password. Returns JWT tokens.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginUserDto loginUserDto) {
        LoginResponse loginResponse = iAuthentication.login(loginUserDto);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginResponse);
    }

    @GetMapping("/oauth-login")
    @Operation(summary = "OAuth login callback", description = "Handles OAuth2 redirect from Google/GitHub. Redirects to frontend with token.")
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

    @Deprecated
    @PostMapping("/register")
    @Operation(summary = "[DEPRECATED] Register with email", description = "Use POST /api/v1/otp/verify instead. Phone OTP signup replaces email registration.")
    public ResponseEntity<ApiResponse> signup(@Valid @RequestBody RegisterUserDto registerUserDto) {
        iAuthentication.signup(registerUserDto);
        ApiResponse apiResponse = new ApiResponse("Code sent");
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @Deprecated
    @PutMapping("/verify")
    @Operation(summary = "[DEPRECATED] Verify email", description = "Use POST /api/v1/otp/verify instead. OTP verification replaces email verification.")
    public ResponseEntity<ApiResponse> verifyUser(@Valid @RequestBody VerifyRequest request) {
        iAuthentication.verifyUser(request.getEmail(), request.getVerificationCode());
        ApiResponse apiResponse = new ApiResponse("Account verified successfully");
        return ResponseEntity.ok().body(apiResponse);
    }

    @Deprecated
    @GetMapping("/resend-code")
    @Operation(summary = "[DEPRECATED] Resend verification code", description = "Use POST /api/v1/otp/send instead. OTP replaces email codes.")
    public ResponseEntity<ApiResponse> resendVerificationCode(@RequestParam String email) {
        iAuthentication.resendVerificationCode(email);
        ApiResponse apiResponse = new ApiResponse("Verification code resent");
        return ResponseEntity.ok().body(apiResponse);
    }

    @Deprecated
    @GetMapping("/reset-password-request")
    @Operation(summary = "[DEPRECATED] Request password reset", description = "Password reset via email is deprecated. OTP-based auth uses phone verification.")
    public ResponseEntity<ApiResponse> requestPasswordReset(@RequestParam String email) {
        iAuthentication.requestPasswordReset(email);
        ApiResponse apiResponse = new ApiResponse("Password reset email sent");
        return ResponseEntity.ok().body(apiResponse);
    }

    @Deprecated
    @PutMapping("/reset-password")
    @Operation(summary = "[DEPRECATED] Reset password", description = "Password reset via email is deprecated. OTP-based auth uses phone verification.")
    public ResponseEntity<ApiResponse> resetPassword(@RequestParam String email, @RequestParam String verificationCode, @RequestParam String newPassword) {
        iAuthentication.resetPassword(email, verificationCode, newPassword);
        ApiResponse apiResponse = new ApiResponse("Password reset successful");
        return ResponseEntity.ok().body(apiResponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange a valid refresh token for a new access token.")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse loginResponse = iAuthentication.refresh(request.getRefreshToken());
        return ResponseEntity.ok(loginResponse);
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update profile", description = "Update user profile fields, skills, and account settings. All fields are optional.")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        UserResponse response = iAuthentication.updateProfile(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Blacklist the access token and revoke all refresh tokens for the user.")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponse("Invalid token"));
        }
        String token = authorizationHeader.substring(7);
        iAuthentication.logout(token);
        return ResponseEntity.ok(new ApiResponse("Logout successful"));
    }
}
