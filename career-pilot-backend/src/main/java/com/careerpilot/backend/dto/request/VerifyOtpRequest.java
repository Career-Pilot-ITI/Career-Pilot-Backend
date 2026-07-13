package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {
  @NotBlank(message = "Phone number is required")
  private String phoneNumber;

  @NotBlank(message = "Verification code is required")
  private String code;
}
