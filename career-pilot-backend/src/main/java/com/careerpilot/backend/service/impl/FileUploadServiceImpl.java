package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.response.UserFileResponse;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.entity.UserFile;
import com.careerpilot.backend.repository.IUserFileRepository;
import com.careerpilot.backend.repository.IUserRepository;
import com.careerpilot.backend.service.IFileUploadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements IFileUploadService {

    private final IUserFileRepository userFileRepository;
    private final IUserRepository userRepository;
    private final Path uploadDir;

    public FileUploadServiceImpl(IUserFileRepository userFileRepository,
                                  IUserRepository userRepository,
                                  @Value("${app.upload.path:./uploads}") String uploadPath) {
        this.userFileRepository = userFileRepository;
        this.userRepository = userRepository;
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public UserFileResponse upload(MultipartFile file, String type, Long userId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }

        String filename = UUID.randomUUID() + extension;
        String subDir = type != null ? type : "other";
        Path targetDir = uploadDir.resolve(subDir).resolve(String.valueOf(userId));

        try {
            Files.createDirectories(targetDir);
            Path targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            User user = userRepository.getReferenceById(userId);

            UserFile userFile = new UserFile();
            userFile.setUser(user);
            userFile.setType(type);
            userFile.setOriginalName(originalName);
            userFile.setStoredPath("/api/v1/files/" + subDir + "/" + userId + "/" + filename);
            userFile.setContentType(file.getContentType());
            userFile.setSizeBytes(file.getSize());
            userFile.setCreatedAt(LocalDateTime.now());

            userFile = userFileRepository.save(userFile);
            return UserFileResponse.from(userFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }
}
