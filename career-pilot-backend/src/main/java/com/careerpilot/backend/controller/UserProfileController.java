package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.UserProfileResponse;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import com.careerpilot.backend.security.SecurityUtil;
import com.careerpilot.backend.service.IUserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "View and update user profile, skills, and CV")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final IUserProfileService userProfileService;
    private final SecurityUtil securityUtil;

    @GetMapping
    @Operation(summary = "Get profile", description = "Returns the authenticated user's profile with skills and CV info")
    public ResponseEntity<UserProfileResponse> getProfile() {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PatchMapping
    @Operation(summary = "Update profile", description = "Update profile fields, skills, and account settings. All fields are optional.")
    public ResponseEntity<UserProfileResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }

    @PostMapping(value = "/cv/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and analyze CV",
            description = "Upload a PDF/DOCX CV. Extracts text via Tika, analyzes via LLM, saves skills and profile fields.")
    public ResponseEntity<UserProfileResponse> analyzeCv(@RequestPart("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Long userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(userProfileService.analyzeCv(userId, file));
    }
}
