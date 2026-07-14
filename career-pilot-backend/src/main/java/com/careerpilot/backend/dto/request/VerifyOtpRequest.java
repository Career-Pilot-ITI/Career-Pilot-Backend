package com.careerpilot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Schema(example = "+201234567890")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Schema(example = "123456")
    private String code;
}
