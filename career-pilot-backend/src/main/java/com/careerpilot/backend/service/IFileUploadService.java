package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.response.UserFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IFileUploadService {
    UserFileResponse upload(MultipartFile file, String type, Long userId);
}
