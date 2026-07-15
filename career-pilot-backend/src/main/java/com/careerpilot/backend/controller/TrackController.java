package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.TrackResponse;
import com.careerpilot.backend.dto.request.CreateTrackRequest;
import com.careerpilot.backend.dto.request.UpdateTrackRequest;
import com.careerpilot.backend.service.ITrackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tracks", description = "Career tracks CRUD (admin) and listing (public)")
public class TrackController {

    private final ITrackService trackService;

    @GetMapping("/api/v1/tracks")
    @Operation(summary = "List active tracks", description = "Public endpoint. Returns only active tracks for user onboarding.")
    public ResponseEntity<List<TrackResponse>> listActive() {
        return ResponseEntity.ok(trackService.findAllActive());
    }

    @GetMapping("/api/v1/admin/tracks")
    @Operation(summary = "List all tracks", description = "Admin only. Paginated list of all tracks including inactive.")
    public ResponseEntity<Page<TrackResponse>> listAll(Pageable pageable) {
        return ResponseEntity.ok(trackService.findAll(pageable));
    }

    @GetMapping("/api/v1/admin/tracks/{id}")
    @Operation(summary = "Get track by ID", description = "Admin only.")
    public ResponseEntity<TrackResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(trackService.findById(id));
    }

    @PostMapping("/api/v1/admin/tracks")
    @Operation(summary = "Create track", description = "Admin only.")
    public ResponseEntity<TrackResponse> create(@Valid @RequestBody CreateTrackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackService.create(request));
    }

    @PutMapping("/api/v1/admin/tracks/{id}")
    @Operation(summary = "Update track", description = "Admin only.")
    public ResponseEntity<TrackResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTrackRequest request) {
        return ResponseEntity.ok(trackService.update(id, request));
    }

    @PatchMapping("/api/v1/admin/tracks/{id}/deactivate")
    @Operation(summary = "Deactivate track", description = "Admin only. Soft-deletes the track.")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        trackService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/v1/admin/tracks/{id}/activate")
    @Operation(summary = "Activate track", description = "Admin only. Re-activates a deactivated track.")
    public ResponseEntity<Void> activate(@PathVariable Long id) {
        trackService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/v1/admin/tracks/{id}")
    @Operation(summary = "Delete track", description = "Admin only. Fails if the track has associated questions or sessions.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
