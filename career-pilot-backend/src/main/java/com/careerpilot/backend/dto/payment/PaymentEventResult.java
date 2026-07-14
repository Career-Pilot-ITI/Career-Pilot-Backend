package com.careerpilot.backend.dto.payment;

import lombok.Data;

@Data
public class PaymentEventResult {
    private boolean valid;
    private boolean success;
    private String merchantOrderId;
    private String providerTransactionId;
    private String rawPayload;
}