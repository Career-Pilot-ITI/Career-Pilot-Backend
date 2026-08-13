package com.careerpilot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164 format, e.g. +201234567890")
    @Schema(example = "+201234567890")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Schema(example = "123456")
    private String code;
}
