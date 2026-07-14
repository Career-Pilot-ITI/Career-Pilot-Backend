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

    public static UserResponse from(User user, UserProfile profile, boolean isNewUser) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setPhoneNumber(user.getPhoneNumber());
        r.setNewUser(isNewUser);
        r.setProfile(profile != null ? UserProfileResponse.from(profile) : new UserProfileResponse());
        return r;
    }
}
