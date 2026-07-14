package com.careerpilot.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpAuthResponse {
    private AuthTokensResponse authTokens;
    private UserResponse user;
}
