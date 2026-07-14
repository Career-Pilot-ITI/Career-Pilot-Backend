package com.careerpilot.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthTokensResponse {
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}
