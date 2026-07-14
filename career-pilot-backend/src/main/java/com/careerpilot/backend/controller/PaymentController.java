package com.careerpilot.backend.controller;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.dto.request.InitiatePaymentRequest;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final IPaymentService paymentService;

    @PostMapping("/initiate")
    @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
    public PaymentInitiationResponse initiate(@Valid @RequestBody InitiatePaymentRequest request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return paymentService.initiatePayment(
                userDetails.getUser(), request.getAmount(), request.getCurrency(), request.getMethod());
    }

    @PostMapping("/webhook/{provider}")
    public void webhook(@PathVariable String provider,
                        @RequestParam Map<String, String> queryParams,
                        @RequestBody String rawBody) {
        paymentService.handleWebhook(provider.toUpperCase(), rawBody, queryParams);
    }
}