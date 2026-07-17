package com.careerpilot.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user");
        }
        Object details = auth.getDetails();
        if (details instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("Could not extract user ID from authentication");
    }
}
