package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
  @NotBlank(message = "Phone number is required")
  @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$", message = "Phone number must be in E.164 format, e.g. +201234567890")
  private String phoneNumber;
}
