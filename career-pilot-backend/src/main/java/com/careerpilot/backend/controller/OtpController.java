package com.careerpilot.backend.controller;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.controller.response.OtpAuthResponse;
import com.careerpilot.backend.dto.request.SendOtpRequest;
import com.careerpilot.backend.dto.request.VerifyOtpRequest;
import com.careerpilot.backend.service.IAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/otp")
@RequiredArgsConstructor
@Tag(name = "OTP Authentication", description = "Phone-based OTP signup and authentication")
public class OtpController {

    private final IAuthentication iAuthentication;

    @PostMapping("/send")
    @RateLimit(capacity = 3, refillTokens = 3, refillSeconds = 60, key = "#request.phoneNumber")
    @Operation(summary = "Send OTP", description = "Send a one-time password to the user's phone number via SMS or WhatsApp.")
    public ResponseEntity<ApiResponse> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        iAuthentication.sendOtp(request.getPhoneNumber());
        return ResponseEntity.ok(new ApiResponse("OTP sent"));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify OTP", description = "Verify the OTP code. If the user is new, creates an account. Returns auth tokens and user info with isNewUser flag.")
    public ResponseEntity<OtpAuthResponse> verify(@Valid @RequestBody VerifyOtpRequest request) {
        OtpAuthResponse response = iAuthentication.verifyOtp(request);
        return ResponseEntity.ok(response);
    }
}
