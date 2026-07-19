package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DowngradeSubscriptionRequest {
    @NotBlank
    private String tier; // must be lower than current tier
}