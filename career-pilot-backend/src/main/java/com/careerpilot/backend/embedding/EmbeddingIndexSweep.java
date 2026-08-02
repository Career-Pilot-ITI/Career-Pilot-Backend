package com.careerpilot.backend.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Kicks off the one-off embedding backfill once the application is ready.
 *
 * <p>The actual work runs on the async executor ({@link EmbeddingIndexService#reindexAll()})
 * so boot is never blocked, and it self-skips when the outsourced {@code vector_store}
 * table is already populated.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingIndexSweep {

    private final EmbeddingIndexService embeddingIndexService;

    @Value("${app.embedding.sweep-on-startup:false}")
    private boolean sweepOnStartup;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!sweepOnStartup) {
            log.debug("Embedding sweep on startup disabled (app.embedding.sweep-on-startup=false)");
            return;
        }
        embeddingIndexService.reindexAll();
    }
}
