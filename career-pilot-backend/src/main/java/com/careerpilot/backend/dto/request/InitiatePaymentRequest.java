package com.careerpilot.backend.dto.request;

import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitiatePaymentRequest {

    @NotNull
    @DecimalMin(value = "0.01")
    private Double amount;

    @NotBlank
    private String currency;

    @NotBlank
    private String method; // "card" | "wallet" | "meeza"

    @NotNull
    private PaymentProvider provider;
}