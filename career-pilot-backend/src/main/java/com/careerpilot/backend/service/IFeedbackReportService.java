package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.FeedbackReportResponse;

public interface IFeedbackReportService {

    /**
     * The terminal call of the interview lifecycle.
     *
     * - Marks session COMPLETED if still IN_PROGRESS.
     * - Generates and persists a FeedbackReport if one doesn't exist.
     * - Returns the saved report on subsequent calls (idempotent).
     */
    FeedbackReportResponse getFeedbackReport(Long sessionId, Long userId);
}
