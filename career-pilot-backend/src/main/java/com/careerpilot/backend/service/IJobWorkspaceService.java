package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.request.ImportJobTextRequest;
import com.careerpilot.backend.dto.request.ImportJobUrlRequest;
import com.careerpilot.backend.dto.response.JobWorkspaceResponse;
import com.careerpilot.backend.entity.ENUMs.JobWorkspaceStatus;

import java.util.List;

public interface IJobWorkspaceService {

    JobWorkspaceResponse importFromText(Long userId, ImportJobTextRequest request);

    JobWorkspaceResponse importFromUrl(Long userId, ImportJobUrlRequest request);

    List<JobWorkspaceResponse> listWorkspaces(Long userId);

    JobWorkspaceResponse getWorkspace(Long id, Long userId);

    void deleteWorkspace(Long id, Long userId);

    JobWorkspaceResponse updateStatus(Long id, Long userId, JobWorkspaceStatus status);
}
