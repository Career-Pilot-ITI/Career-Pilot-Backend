package com.careerpilot.backend.dto.payment;

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
}