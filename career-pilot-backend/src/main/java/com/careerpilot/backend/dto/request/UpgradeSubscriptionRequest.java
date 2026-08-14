package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpgradeSubscriptionRequest {
    @NotBlank
    private String tier; // "PLUS" | "PRO"

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. EGP or USD")
    private String currency;

    @NotBlank
    private String method;
}