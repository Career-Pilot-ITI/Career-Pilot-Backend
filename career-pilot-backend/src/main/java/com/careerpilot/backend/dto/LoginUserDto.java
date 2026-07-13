package com.careerpilot.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginUserDto {

    @NotBlank(message = "Email or username is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;
}
