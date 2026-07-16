package com.careerpilot.backend.controller;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.controller.response.PaymentTransactionResponse;
import com.careerpilot.backend.dto.request.InitiatePaymentRequest;
import com.careerpilot.backend.entity.PaymentTransaction;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IPaymentService;
import com.careerpilot.backend.service.IPaymentTransactionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Paymob checkout initiation and webhook handling")
public class PaymentController {

    private final IPaymentService paymentService;
    private final IPaymentTransactionService paymentTransactionService;

    @GetMapping("/history")
    public Page<PaymentTransactionResponse> history(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                    Pageable pageable) {
        return paymentTransactionService.findByUser(userDetails.getUser().getId(), pageable)
                .map(PaymentTransactionResponse::from);
    }

    @PostMapping("/initiate")
    @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
    public PaymentInitiationResponse initiate(@Valid @RequestBody InitiatePaymentRequest request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return paymentService.initiatePayment(
                userDetails.getUser(), request.getAmount(), request.getCurrency(), request.getMethod(),
                request.getProvider(), request.getPurchaseType(), request.getCoinPackSize(), request.getTier());
    }


    @PostMapping("/webhook/{provider}")
    public void webhook(@PathVariable String provider,
                        @RequestParam Map<String, String> queryParams,
                        @RequestBody String rawBody) {
        paymentService.handleWebhook(provider.toUpperCase(), rawBody, queryParams);
    }
}