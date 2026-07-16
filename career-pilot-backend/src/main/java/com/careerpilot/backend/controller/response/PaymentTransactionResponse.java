package com.careerpilot.backend.controller.response;

import com.careerpilot.backend.entity.PaymentTransaction;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentTransactionResponse {
    private Long id;
    private Double amount;
    private String currency;
    private String status;
    private String paymentMethod;
    private String provider;
    private String merchantOrderId;
    private String providerTransactionId;
    private String failureReason;
    private Integer coinPackSize;
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