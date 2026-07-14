package com.careerpilot.backend.utils;

import com.careerpilot.backend.config.PaymobConfig;
import com.careerpilot.backend.controller.advice.PaymentException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymobClient {

    private final RestTemplate restTemplate;
    private final PaymobConfig config;

    @SuppressWarnings("unchecked")
    public String createIntention(long amountCents, String currency, String merchantOrderId, String paymentMethodKey) {
        Integer integrationId = config.getIntegrationIds().get(paymentMethodKey);
        if (integrationId == null) {
            throw new PaymentException.UnsupportedPaymentProviderException(
                    "No Paymob integration configured for method: " + paymentMethodKey);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Token " + config.getSecretKey());

        Map<String, Object> body = Map.of(
                "amount", amountCents,
                "currency", currency,
                "payment_methods", List.of(integrationId),
                "special_reference", merchantOrderId,
                "notification_url", config.getNotificationUrl(),
                "redirection_url", config.getRedirectionUrl()
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/v1/intention/", request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || responseBody.get("client_secret") == null) {
                throw new PaymentException.PaymentProviderException("Paymob returned no client_secret");
            }
            return (String) responseBody.get("client_secret");
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