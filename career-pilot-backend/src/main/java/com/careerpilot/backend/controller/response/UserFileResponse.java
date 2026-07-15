package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.UserFile;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserFileResponse {
    private Long id;
    private String type;
    private String originalName;
    private String url;
    private Long sizeBytes;
    private LocalDateTime createdAt;

    public static UserFileResponse from(UserFile userFile) {
        UserFileResponse r = new UserFileResponse();
        r.setId(userFile.getId());
        r.setType(userFile.getType());
        r.setOriginalName(userFile.getOriginalName());
        r.setUrl(userFile.getStoredPath());
        r.setSizeBytes(userFile.getSizeBytes());
        r.setCreatedAt(userFile.getCreatedAt());
        return r;
    }
}
