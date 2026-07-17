package com.careerpilot.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopUpRequest {
    @NotNull
    private Integer coinPackSize;

    @NotBlank
    private String currency;

    @NotBlank
    private String method;
}