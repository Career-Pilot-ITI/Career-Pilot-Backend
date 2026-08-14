package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TopUpRequest {
    @NotNull
    @Positive(message = "coinPackSize must be greater than 0")
    private Integer coinPackSize;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code, e.g. EGP or USD")
    private String currency;

    @NotBlank
    private String method;
}