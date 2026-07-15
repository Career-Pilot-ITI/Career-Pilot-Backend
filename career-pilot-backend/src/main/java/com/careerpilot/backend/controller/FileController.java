package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.UserFileResponse;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IFileUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload for avatars, resumes, and CVs")
public class FileController {

    private final IFileUploadService fileUploadService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file", description = "Upload an avatar, resume, or CV file. Returns the file ID and URL to use in profile update.")
    public ResponseEntity<UserFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("type") @Parameter(description = "File type", schema = @Schema(allowableValues = {"avatars", "resumes", "cvs"})) String type) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        UserFileResponse response = fileUploadService.upload(file, type, userDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }
}
