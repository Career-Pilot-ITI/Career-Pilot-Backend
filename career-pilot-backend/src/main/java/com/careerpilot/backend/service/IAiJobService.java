package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.AiJobResponse;

public interface IAiJobService {
    AiJobResponse getJobStatus(Long jobId, Long userId);
}
