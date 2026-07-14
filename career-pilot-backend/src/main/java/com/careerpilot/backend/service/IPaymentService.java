package com.careerpilot.backend.service;

import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.entity.User;

import java.util.Map;

public interface IPaymentService {
    PaymentInitiationResponse initiatePayment(User user, double amount, String currency, String method);
    void handleWebhook(String provider, String rawBody, Map<String, String> queryParams);
}