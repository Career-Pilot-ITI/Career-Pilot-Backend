package com.careerpilot.backend.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationRequest {
    private Long userId;
    private long amountCents;
    private String currency;
    private String merchantOrderId;
    private String paymentMethod;
    private String username;
    private String email;
    private String phoneNumber;
    @Schema(description = "Type of purchase being made", example = "COIN_PACK", allowableValues = {"COIN_PACK", "SUBSCRIPTION"})
    private String purchaseType;
    private Integer coinPackSize;
    private String tier;
}