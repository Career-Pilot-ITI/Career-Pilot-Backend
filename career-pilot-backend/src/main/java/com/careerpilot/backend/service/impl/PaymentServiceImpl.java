package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.PaymentException;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.dto.payment.PaymentEventResult;
import com.careerpilot.backend.dto.payment.PaymentInitiationRequest;
import com.careerpilot.backend.dto.payment.PaymentInitiationResult;
import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.PaymentTransaction;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static com.careerpilot.backend.entity.ENUMs.PaymentStatus.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentProviderResolver providerResolver;
    private final IPaymentTransactionService transactionService;
    private final ICoinWalletService coinWalletService;
    private final ISubscriptionService subscriptionService;

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(User user, double amount, String currency, String method,
                                                     PaymentProvider paymentProvider, String purchaseType,
                                                     Integer coinPackSize, String tier) {
        String merchantOrderId = "CP-" + user.getId() + "-" + UUID.randomUUID();

        PaymentInitiationRequest req = new PaymentInitiationRequest(
                user.getId(), Math.round(amount * 100), currency, merchantOrderId, method,
                user.getUsername(), user.getEmail(), user.getPhoneNumber(),
                purchaseType, coinPackSize, tier);

        IPaymentProvider provider = providerResolver.resolve(paymentProvider.toString());
        PaymentInitiationResult result = provider.initiate(req);

        transactionService.createPending(user, amount, currency, method,
                provider.getProviderName(), merchantOrderId, coinPackSize, tier);

        return new PaymentInitiationResponse(result.getCheckoutUrl(), merchantOrderId);
    }

    @Override
    @Transactional
    public void handleWebhook(String providerKey, String rawBody, Map<String, String> queryParams) {
        IPaymentProvider provider = providerResolver.resolve(providerKey);
        PaymentEventResult event = provider.parseAndVerifyWebhook(rawBody, queryParams);

        if (!event.isValid()) {
            throw new PaymentException.InvalidWebhookSignatureException(
                    "Webhook failed verification for provider: " + providerKey);
        }

        PaymentTransaction tx = transactionService.findByMerchantOrderId(event.getMerchantOrderId())
                .orElseThrow(() -> new PaymentException.UnknownTransactionException(
                        "Unknown order: " + event.getMerchantOrderId()));

        if (tx.getStatus() != PENDING) {
            return; // idempotency guard against Paymob webhook retries
        }

        if (event.isSuccess()) {
            transactionService.markConfirmed(tx, event.getProviderTransactionId(), event.getRawPayload());
            if (tx.getCoinPackSize() != null) {
                coinWalletService.credit(tx.getUser().getId(), tx.getCoinPackSize());
            } else if (tx.getTierPurchased() != null) {
                SubscriptionTier tier = SubscriptionTier.valueOf(tx.getTierPurchased());
                subscriptionService.upgrade(tx.getUser(), tier);
            }
        } else {
            transactionService.markFailed(tx, event.getProviderTransactionId(), event.getRawPayload(), event.getFailureReason());
        }
    }
}