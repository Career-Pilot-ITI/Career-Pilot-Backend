package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.PaymentTransaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "A single payment transaction record")
public class PaymentTransactionResponse {

    @Schema(example = "18")
    private Long id;

    @Schema(example = "10.0")
    private Double amount;

    @Schema(example = "EGP")
    private String currency;

    @Schema(description = "PENDING, CONFIRMED, FAILED, or REFUNDED", example = "CONFIRMED")
    private String status;

    @Schema(example = "card")
    private String paymentMethod;

    @Schema(example = "PAYMOB")
    private String provider;

    @Schema(example = "CP-1-6dfe38d0-5f3b-42e1-8905-62775bd76260")
    private String merchantOrderId;

    @Schema(description = "Transaction id assigned by the payment provider", example = "496966996")
    private String providerTransactionId;

    @Schema(description = "Populated only when status is FAILED", example = "Do not honour")
    private String failureReason;

    @Schema(description = "Populated only if purchaseType was COIN_PACK", example = "500")
    private Integer coinPackSize;

    @Schema(description = "Populated only if purchaseType was SUBSCRIPTION", example = "PRO")
    private String tierPurchased;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;

    public static PaymentTransactionResponse from(PaymentTransaction tx) {
        PaymentTransactionResponse dto = new PaymentTransactionResponse();
        dto.setId(tx.getId());
        dto.setAmount(tx.getAmount());
        dto.setCurrency(tx.getCurrency());
        dto.setStatus(tx.getStatus() != null ? tx.getStatus().toString() : null);
        dto.setPaymentMethod(tx.getPaymentMethod());
        dto.setProvider(tx.getProvider());
        dto.setMerchantOrderId(tx.getMerchantOrderId());
        dto.setProviderTransactionId(tx.getProviderTransactionId());
        dto.setFailureReason(tx.getFailureReason());
        dto.setCoinPackSize(tx.getCoinPackSize());
        dto.setTierPurchased(tx.getTierPurchased());
        dto.setCreatedAt(tx.getCreatedAt());
        dto.setConfirmedAt(tx.getConfirmedAt());
        return dto;
    }
}