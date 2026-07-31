package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.dto.request.ImportJobTextRequest;
import com.careerpilot.backend.dto.request.ImportJobUrlRequest;
import com.careerpilot.backend.dto.request.UpdateWorkspaceStatusRequest;
import com.careerpilot.backend.dto.response.JobWorkspaceResponse;
import com.careerpilot.backend.security.SecurityUtil;
import com.careerpilot.backend.service.IJobWorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Job Workspaces", description = "Import jobs, manage saved-job workspaces, and drive job-scoped AI services")
@SecurityRequirement(name = "bearerAuth")
public class JobWorkspaceController {

    private final IJobWorkspaceService workspaceService;
    private final SecurityUtil securityUtil;

    @PostMapping("/import/text")
    @Operation(summary = "Import a job from pasted text",
            description = "Parses raw job description text via LLM, saves the JobListing, and creates a JobWorkspace.")
    public ResponseEntity<ApiResponse> importFromText(@Valid @RequestBody ImportJobTextRequest request) {
        JobWorkspaceResponse workspace = workspaceService.importFromText(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .success(true)
                .message("Job imported from text")
                .data(workspace)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/import/url")
    @Operation(summary = "Import a job from a URL",
            description = "Fetches the job posting (LinkedIn via ChocoData, others via HTML fetch), parses it, and creates a JobWorkspace. Reuses a cached job when the source URL was already imported.")
    public ResponseEntity<ApiResponse> importFromUrl(@Valid @RequestBody ImportJobUrlRequest request) {
        JobWorkspaceResponse workspace = workspaceService.importFromUrl(securityUtil.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.builder()
                .success(true)
                .message("Job imported from URL")
                .data(workspace)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping
    @Operation(summary = "List the user's job workspaces")
    public ResponseEntity<ApiResponse> listWorkspaces() {
        List<JobWorkspaceResponse> workspaces = workspaceService.listWorkspaces(securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Workspaces retrieved")
                .data(workspaces)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single job workspace")
    public ResponseEntity<ApiResponse> getWorkspace(@PathVariable Long id) {
        JobWorkspaceResponse workspace = workspaceService.getWorkspace(id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Workspace retrieved")
                .data(workspace)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job workspace")
    public ResponseEntity<ApiResponse> deleteWorkspace(@PathVariable Long id) {
        workspaceService.deleteWorkspace(id, securityUtil.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Workspace deleted")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update a workspace's application status",
            description = "Moves the workspace through the pipeline: SAVED, APPLYING, INTERVIEWING, OFFER, ARCHIVED.")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody UpdateWorkspaceStatusRequest request) {
        JobWorkspaceResponse workspace = workspaceService.updateStatus(
                id, securityUtil.getCurrentUserId(), request.getStatus());
        return ResponseEntity.ok(ApiResponse.builder()
                .success(true)
                .message("Workspace status updated")
                .data(workspace)
                .timestamp(LocalDateTime.now())
                .build());
    }
}
