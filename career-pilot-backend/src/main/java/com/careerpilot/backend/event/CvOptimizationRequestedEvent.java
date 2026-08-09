package com.careerpilot.backend.event;

import com.careerpilot.backend.entity.JobListing;

/**
 * Published inside the CV-optimization request transaction; consumed by
 * {@link CvOptimizationEventListener} after commit so the async worker can see
 * the persisted job row.
 */
public record CvOptimizationRequestedEvent(
    Long jobId,
    Long userId,
    String rawCvText,
    JobListing job,
    int coinCost
) {
}
