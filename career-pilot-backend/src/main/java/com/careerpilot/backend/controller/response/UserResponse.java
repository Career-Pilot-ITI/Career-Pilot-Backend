package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.entity.UserProfile;
import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String phoneNumber;
    private boolean isNewUser;
    private UserProfileResponse profile;
}
