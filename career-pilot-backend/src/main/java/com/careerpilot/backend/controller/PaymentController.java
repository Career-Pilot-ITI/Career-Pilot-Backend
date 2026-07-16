package com.careerpilot.backend.controller;

import com.careerpilot.backend.annotation.RateLimit;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.controller.response.PaymentTransactionResponse;
import com.careerpilot.backend.dto.request.InitiatePaymentRequest;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.IPaymentService;
import com.careerpilot.backend.service.IPaymentTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
@Tag(name = "Payments", description = "Paymob checkout initiation, webhook confirmation, and transaction history")
public class PaymentController {

    private final IPaymentService paymentService;
    private final IPaymentTransactionService paymentTransactionService;

    @PostMapping("/initiate")
    @RateLimit(capacity = 5, refillTokens = 5, refillSeconds = 60)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Initiate a Paymob checkout",
            description = "Creates a payment intention with Paymob and returns a hosted checkout URL to open in a " +
                    "WebView or browser. Requires exactly one of coinPackSize or tier depending on purchaseType. " +
                    "A PENDING payment transaction is created immediately; it is later confirmed or failed via the " +
                    "webhook endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkout URL created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed (missing purchaseType, invalid amount, mismatched coinPackSize/tier, etc.)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded (5 requests per 60s)", content = @Content),
            @ApiResponse(responseCode = "502", description = "Paymob rejected the intention request", content = @Content)
    })
    public PaymentInitiationResponse initiate(@Valid @RequestBody InitiatePaymentRequest request,
                                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        return paymentService.initiatePayment(
                userDetails.getUser(), request.getAmount(), request.getCurrency(), request.getMethod(),
                request.getProvider(), request.getPurchaseType(), request.getCoinPackSize(), request.getTier());
    }

    @PostMapping("/webhook/{provider}")
    @Operation(
            summary = "Payment provider webhook callback",
            description = "Server-to-server callback invoked by the payment provider (e.g. Paymob) after a payment " +
                    "attempt completes. Not authenticated via JWT — verified instead via HMAC signature. " +
                    "Idempotent: replayed webhooks for an already-processed transaction are safely ignored."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Webhook processed (or safely ignored if already handled)"),
            @ApiResponse(responseCode = "401", description = "Invalid HMAC signature", content = @Content),
            @ApiResponse(responseCode = "404", description = "No matching transaction found for the webhook's merchant order id", content = @Content)
    })
    public void webhook(
            @Parameter(description = "Payment provider identifier, e.g. 'paymob'", example = "paymob")
            @PathVariable String provider,
            @Parameter(description = "Provider-specific query parameters, e.g. Paymob's hmac signature")
            @RequestParam Map<String, String> queryParams,
            @Parameter(description = "Raw JSON request body, preserved unparsed for HMAC verification")
            @RequestBody String rawBody) {
        paymentService.handleWebhook(provider.toUpperCase(), rawBody, queryParams);
    }

    @GetMapping("/history")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Get payment transaction history",
            description = "Returns a paginated list of the authenticated user's own payment transactions, most " +
                    "recent first, regardless of status (PENDING, CONFIRMED, FAILED, REFUNDED)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction history returned"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT", content = @Content)
    })
    public Page<PaymentTransactionResponse> history(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        return paymentTransactionService.findByUser(userDetails.getUser().getId(), pageable)
                .map(PaymentTransactionResponse::from);
    }
}