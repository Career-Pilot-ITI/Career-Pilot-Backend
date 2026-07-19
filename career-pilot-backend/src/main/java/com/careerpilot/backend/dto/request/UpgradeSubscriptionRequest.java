package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpgradeSubscriptionRequest {
    @NotBlank
    private String tier; // "PLUS" | "PRO"

    @NotBlank
    private String currency;

    @NotBlank
    private String method;
}