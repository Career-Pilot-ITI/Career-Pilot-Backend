package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.AtsScoreResponse;
import com.careerpilot.backend.dto.response.CoverLetterResponse;
import com.careerpilot.backend.dto.response.CvOptimizationResponse;

public interface IJobWorkspaceAiService {

    AtsScoreResponse scoreCv(Long workspaceId, Long userId);

    CvOptimizationResponse optimizeCv(Long workspaceId, Long userId);

    CoverLetterResponse generateCoverLetter(Long workspaceId, Long userId);
}
