package com.careerpilot.backend.dto.request;

import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to initiate a Paymob checkout for either a coin pack or a subscription tier")
public class InitiatePaymentRequest {

    @Schema(description = "Purchase amount in major currency units (not cents)", example = "10.00")
    @NotNull
    @DecimalMin(value = "0.01")
    private Double amount;

    @Schema(description = "ISO currency code", example = "EGP")
    @NotBlank
    private String currency;

    @Schema(description = "Payment method to use", example = "card", allowableValues = {"card", "wallet", "meeza"})
    @NotBlank
    private String method;

    @Schema(description = "Payment provider to route through", example = "PAYMOB")
    @NotNull
    private PaymentProvider provider;

    @Schema(description = "What is being purchased", example = "COIN_PACK", allowableValues = {"COIN_PACK", "SUBSCRIPTION"})
    @NotBlank
    private String purchaseType;

    @Schema(description = "Number of coins in the pack. Required when purchaseType is COIN_PACK.", example = "500")
    private Integer coinPackSize;

    @Schema(description = "Subscription tier being purchased. Required when purchaseType is SUBSCRIPTION.", example = "PRO")
    private String tier;

    @AssertTrue(message = "coinPackSize is required when purchaseType is COIN_PACK, tier is required when purchaseType is SUBSCRIPTION")
    @Schema(hidden = true)
    public boolean isPurchaseDetailValid() {
        if ("COIN_PACK".equals(purchaseType)) {
            return coinPackSize != null;
        }
        if ("SUBSCRIPTION".equals(purchaseType)) {
            return tier != null && !tier.isBlank();
        }
        return false;
    }
}