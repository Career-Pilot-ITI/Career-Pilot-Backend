package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.AiJobException;
import com.careerpilot.backend.dto.response.AiJobResponse;
import com.careerpilot.backend.entity.AiJob;
import com.careerpilot.backend.repository.IAiJobRepository;
import com.careerpilot.backend.service.IAiJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJobServiceImpl implements IAiJobService {

    private final IAiJobRepository aiJobRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public AiJobResponse getJobStatus(Long jobId, Long userId) {
        AiJob job = aiJobRepository.findByIdAndUserId(jobId, userId)
            .orElseThrow(() -> new AiJobException.AiJobNotFoundException(
                "AI job not found or access denied: " + jobId));

        return AiJobResponse.from(job, objectMapper);
    }
}
