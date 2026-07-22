package com.careerpilot.backend.controller;

import com.careerpilot.backend.config.CoinPackConfig;
import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.controller.response.CoinBalanceResponse;
import com.careerpilot.backend.controller.response.CoinLedgerEntryResponse;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.dto.request.TopUpRequest;
import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import com.careerpilot.backend.security.jwt.CustomUserDetails;
import com.careerpilot.backend.service.ICoinWalletService;
import com.careerpilot.backend.service.IPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Coin Wallet", description = "Coin balance and top-up via Paymob")
public class CoinWalletController {

    private final ICoinWalletService coinWalletService;
    private final IPaymentService paymentService;
    private final CoinPackConfig coinPackConfig;

    @GetMapping("/balance")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current coin balance")
    public CoinBalanceResponse balance(@AuthenticationPrincipal CustomUserDetails userDetails) {
        int balance = coinWalletService.getBalance(userDetails.getUser().getId());
        return new CoinBalanceResponse(balance);
    }

    @PostMapping("/top-up")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Initiate a coin pack top-up",
            description = "Creates a Paymob checkout for a fixed-price coin pack. Price is determined server-side from coinPackSize, not client-supplied.")
    public PaymentInitiationResponse topUp(@Valid @RequestBody TopUpRequest request,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        Double price = coinPackConfig.getPrices().get(request.getCoinPackSize());
        if (price == null) {
            throw new WalletException.InvalidCoinPackException(
                    "Invalid coin pack size: " + request.getCoinPackSize() + ". Allowed: " + coinPackConfig.getPrices().keySet());
        }

        return paymentService.initiatePayment(
                userDetails.getUser(), price, request.getCurrency(), request.getMethod(),
                PaymentProvider.PAYMOB, "COIN_PACK", request.getCoinPackSize(), null);
    }

    @GetMapping("/ledger")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get coin ledger history", description = "Paginated list of all credits/debits to the user's wallet.")
    public Page<CoinLedgerEntryResponse> ledger(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        return coinWalletService.getLedgerHistory(userDetails.getUser().getId(), pageable)
                .map(CoinLedgerEntryResponse::from);
    }
}