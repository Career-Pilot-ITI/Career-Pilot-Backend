package com.careerpilot.backend.controller;

import com.careerpilot.backend.config.TierPricingConfig;
import com.careerpilot.backend.controller.advice.SubscriptionException;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.controller.response.SubscriptionResponse;
import com.careerpilot.backend.dto.request.DowngradeSubscriptionRequest;
import com.careerpilot.backend.dto.request.UpgradeSubscriptionRequest;
import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IPaymentService;
import com.careerpilot.backend.service.ISubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Tier management and upgrades via Paymob")
public class SubscriptionController {

    private final ISubscriptionService subscriptionService;
    private final IPaymentService paymentService;
    private final TierPricingConfig tierPricingConfig;

    @GetMapping("/current")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current subscription details")
    public SubscriptionResponse current(@AuthenticationPrincipal CustomUserDetails userDetails) {
        Subscription sub = subscriptionService.getCurrentSubscription(userDetails.getUser().getId());
        return SubscriptionResponse.from(sub);
    }

    @PostMapping("/upgrade")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upgrade subscription tier",
            description = "Creates a Paymob checkout for the target tier. Takes effect immediately once payment is confirmed via webhook.")
    public PaymentInitiationResponse upgrade(@Valid @RequestBody UpgradeSubscriptionRequest request,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        Double price = tierPricingConfig.getPrices().get(request.getTier());
        if (price == null) {
            throw new SubscriptionException.InvalidTierException(
                    "Invalid tier: " + request.getTier() + ". Allowed: " + tierPricingConfig.getPrices().keySet());
        }

        return paymentService.initiatePayment(
                userDetails.getUser(), price, request.getCurrency(), request.getMethod(),
                PaymentProvider.PAYMOB, "SUBSCRIPTION", null, request.getTier());
    }

    @PostMapping("/downgrade")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Schedule a downgrade to a lower tier",
            description = "Takes effect at the end of the current billing cycle. No immediate change, no clawback of used quota.")
    public void downgrade(@Valid @RequestBody DowngradeSubscriptionRequest request,
                          @AuthenticationPrincipal CustomUserDetails userDetails) {
        SubscriptionTier target = SubscriptionTier.valueOf(request.getTier());
        subscriptionService.downgrade(userDetails.getUser().getId(), target);
    }

    @PostMapping("/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancel subscription",
            description = "Schedules cancellation at the end of the current billing cycle. No immediate change, no mid-cycle clawback.")
    public void cancel(@AuthenticationPrincipal CustomUserDetails userDetails) {
        subscriptionService.cancel(userDetails.getUser().getId());
    }
}