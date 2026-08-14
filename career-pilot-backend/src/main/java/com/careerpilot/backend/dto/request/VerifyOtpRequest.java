package com.careerpilot.backend.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?201[0-9]{9}$", message = "Invalid Egyptian mobile number. Expected format: +201XXXXXXXXX (country 20 + 1 + 9 digits)")
    @Schema(example = "+201234567890")
    private String phoneNumber;

    @NotBlank(message = "OTP code is required")
    @Schema(example = "123456")
    private String code;
}
