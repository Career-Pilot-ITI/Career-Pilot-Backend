package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class DowngradeSubscriptionRequest {
    @NotBlank
    @Pattern(regexp = "^(FREE|PLUS|PRO)$", message = "Invalid tier. Allowed: FREE, PLUS, PRO")
    private String tier; // must be lower than current tier
}