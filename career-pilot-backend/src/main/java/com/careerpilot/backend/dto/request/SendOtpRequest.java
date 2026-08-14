package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\+?201[0-9]{9}$", message = "Invalid Egyptian mobile number. Expected format: +201XXXXXXXXX (country 20 + 1 + 9 digits)")
  private String phoneNumber;
}
