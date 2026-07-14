package com.careerpilot.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentInitiationResponse {
    private String checkoutUrl;
    private String merchantOrderId;
}