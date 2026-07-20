package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.FeedbackReportResponse;

public interface IFeedbackReportService {
    FeedbackReportResponse getFeedbackReport(Long sessionId, Long userId);
    FeedbackReportResponse generateFeedbackReport(Long sessionId, Long userId);
}
