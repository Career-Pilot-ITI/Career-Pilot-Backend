package com.careerpilot.backend.service;

import com.careerpilot.backend.dto.payment.PaymentEventResult;
import com.careerpilot.backend.dto.payment.PaymentInitiationRequest;
import com.careerpilot.backend.dto.payment.PaymentInitiationResult;

import java.util.Map;

public interface IPaymentProvider {
    String getProviderName();
    PaymentInitiationResult initiate(PaymentInitiationRequest request);
    PaymentEventResult parseAndVerifyWebhook(String rawBody, Map<String, String> queryParams);
}