package com.careerpilot.backend.utils;

import com.careerpilot.backend.config.PaymobConfig;
import com.careerpilot.backend.controller.advice.PaymentException;
import com.careerpilot.backend.dto.payment.PaymentInitiationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymobClient {

    private final RestTemplate restTemplate;
    private final PaymobConfig config;

    @SuppressWarnings("unchecked")
    public String createIntention(PaymentInitiationRequest req) {
        Integer integrationId = config.getIntegrationIds().get(req.getPaymentMethod());
        if (integrationId == null) {
            throw new PaymentException.UnsupportedPaymentProviderException(
                    "No Paymob integration configured for method: " + req.getPaymentMethod());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + config.getSecretKey());

        Map<String, Object> billingData = new LinkedHashMap<>();
        billingData.put("first_name", req.getUsername());
        billingData.put("last_name", "N/A");
        billingData.put("email", req.getEmail() != null ? req.getEmail() : "no-email@careerpilot.app");
        billingData.put("phone_number", req.getPhoneNumber() != null ? req.getPhoneNumber() : "+201000000000");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", req.getAmountCents());
        body.put("currency", req.getCurrency());
        body.put("payment_methods", List.of(integrationId));
        body.put("special_reference", req.getMerchantOrderId());
        body.put("notification_url", config.getNotificationUrl());
        body.put("redirection_url", config.getRedirectionUrl());
        body.put("items", List.of(Map.of("name", "Career Pilot Purchase", "amount", req.getAmountCents(), "quantity", 1)));
        body.put("billing_data", billingData);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/v1/intention/", request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("client_secret") == null) {
                throw new PaymentException.PaymentProviderException("Paymob returned no client_secret");
            }
            return (String) responseBody.get("client_secret");
        } catch (HttpClientErrorException e) {
            log.error("Paymob rejected intention request: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new PaymentException.PaymentProviderException(
                    "Paymob rejected the request: " + e.getResponseBodyAsString(), e);
        } catch (PaymentException.PaymentProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new PaymentException.PaymentProviderException("Failed to create Paymob intention", e);
        }
    }
    public String buildCheckoutUrl(String clientSecret) {
        return config.getBaseUrl() + "/unifiedcheckout/?publicKey=" + config.getPublicKey()
                + "&clientSecret=" + clientSecret;
    }
}