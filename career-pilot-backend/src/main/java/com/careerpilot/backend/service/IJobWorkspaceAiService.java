package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.response.AiJobResponse;
import com.careerpilot.backend.dto.response.AtsScoreResponse;
import com.careerpilot.backend.dto.response.CoverLetterResponse;

public interface IJobWorkspaceAiService {

    AtsScoreResponse scoreCv(Long workspaceId, Long userId);

    AiJobResponse optimizeCv(Long workspaceId, Long userId);

    CoverLetterResponse generateCoverLetter(Long workspaceId, Long userId);
}
