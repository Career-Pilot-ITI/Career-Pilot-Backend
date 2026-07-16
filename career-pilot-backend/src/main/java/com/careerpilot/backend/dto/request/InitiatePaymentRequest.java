package com.careerpilot.backend.dto.request;

import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import jakarta.validation.constraints.AssertTrue;
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

    @NotBlank
    private String purchaseType; // "COIN_PACK" | "SUBSCRIPTION"

    private Integer coinPackSize;   // required if purchaseType == COIN_PACK
    private String tier;            // required if purchaseType == SUBSCRIPTION

    @AssertTrue(message = "coinPackSize is required when purchaseType is COIN_PACK, tier is required when purchaseType is SUBSCRIPTION")
    public boolean isPurchaseDetailValid() {
        if ("COIN_PACK".equals(purchaseType)) {
            return coinPackSize != null;
        }
        if ("SUBSCRIPTION".equals(purchaseType)) {
            return tier != null && !tier.isBlank();
        }
        return false; // unknown purchaseType also fails validation
    }
}