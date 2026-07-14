package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.dto.payment.PaymentEventResult;
import com.careerpilot.backend.dto.payment.PaymentInitiationRequest;
import com.careerpilot.backend.dto.payment.PaymentInitiationResult;
import com.careerpilot.backend.security.paymob.PaymobHmacVerifier;
import com.careerpilot.backend.service.IPaymentProvider;
import com.careerpilot.backend.utils.PaymobClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("PAYMOB")
@RequiredArgsConstructor
public class PaymobPaymentProvider implements IPaymentProvider {

    private final PaymobClient paymobClient;
    private final PaymobHmacVerifier hmacVerifier;
    private final ObjectMapper objectMapper;

    @Override
    public String getProviderName() {
        return "PAYMOB";
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationRequest request) {
        String clientSecret = paymobClient.createIntention(
                request.getAmountCents(), request.getCurrency(),
                request.getMerchantOrderId(), request.getPaymentMethod());
        return new PaymentInitiationResult(paymobClient.buildCheckoutUrl(clientSecret), clientSecret);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PaymentEventResult parseAndVerifyWebhook(String rawBody, Map<String, String> queryParams) {
        PaymentEventResult event = new PaymentEventResult();
        event.setRawPayload(rawBody);

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            event.setValid(false);
            return event;
        }

        if (!"TRANSACTION".equalsIgnoreCase(String.valueOf(body.get("type")))) {
            event.setValid(false);
            return event;
        }

        Map<String, Object> obj = (Map<String, Object>) body.get("obj");
        String hmac = queryParams.get("hmac");

        if (obj == null || !hmacVerifier.isValid(obj, hmac)) {
            event.setValid(false);
            return event;
        }

        Map<String, Object> order = (Map<String, Object>) obj.get("order");
        event.setValid(true);
        event.setSuccess(Boolean.TRUE.equals(obj.get("success")));
        event.setMerchantOrderId(String.valueOf(order.get("merchant_order_id")));
        event.setProviderTransactionId(String.valueOf(obj.get("id")));
        return event;
    }
}