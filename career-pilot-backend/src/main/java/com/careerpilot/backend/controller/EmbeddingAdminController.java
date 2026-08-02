package com.careerpilot.backend.controller;

import com.careerpilot.backend.controller.response.ApiResponse;
import com.careerpilot.backend.embedding.EmbeddingIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Embedding", description = "Embedding index administration")
public class EmbeddingAdminController {

    private final EmbeddingIndexService embeddingIndexService;

    @PostMapping("/api/v1/admin/embedding/reindex")
    @Operation(summary = "Rebuild embedding index", description = "Admin only. Asynchronously re-embeds all active tracks and questions into the vector store. No-op when the index is already populated.")
    public ResponseEntity<ApiResponse<Void>> reindex() {
        embeddingIndexService.reindexAll();
        return ResponseEntity.accepted().body(new ApiResponse<>("Embedding reindex triggered"));
    }
}
