package com.careerpilot.backend.utils;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WireWebService {

    private static final Logger log = LoggerFactory.getLogger(WireWebService.class);

    private final RestTemplate restTemplate;

    @Value("${app.wireweb.api-key}")
    private String apiKey;

    @Value("${app.wireweb.session-id}")
    private String sessionId;

    @Value("${app.wireweb.base-url:https://app.wireweb.co.in}")
    private String baseUrl;

    public void sendOtp(String phoneNumber, String code) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("WireWeb API key not configured. Skipping WhatsApp message.");
            return;
        }

        String url = baseUrl + "/api/v1/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = Map.of(
                "sessionId", sessionId,
                "to", phoneNumber,
                "text", "Your Career Pilot verification code is: " + code
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("WhatsApp OTP sent to {}. Response: {} {}", phoneNumber, response.getStatusCode(), response.getBody());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp OTP to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
