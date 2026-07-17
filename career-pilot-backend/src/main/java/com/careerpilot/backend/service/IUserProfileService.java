package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.response.UserProfileResponse;
import com.careerpilot.backend.dto.request.UpdateProfileRequest;
import org.springframework.web.multipart.MultipartFile;

public interface IUserProfileService {

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse analyzeCv(Long userId, MultipartFile file);

    void createDefaultProfile(Long userId);
}
