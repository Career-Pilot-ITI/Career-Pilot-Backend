package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.PaymentException;
import com.careerpilot.backend.controller.response.PaymentInitiationResponse;
import com.careerpilot.backend.dto.payment.PaymentEventResult;
import com.careerpilot.backend.dto.payment.PaymentInitiationRequest;
import com.careerpilot.backend.dto.payment.PaymentInitiationResult;
import com.careerpilot.backend.entity.PaymentTransaction;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.IPaymentTransactionRepository;
import com.careerpilot.backend.service.IPaymentProvider;
import com.careerpilot.backend.service.IPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static com.careerpilot.backend.entity.ENUMs.PaymentStatus.*;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    private final PaymentProviderResolver providerResolver;
    private final IPaymentTransactionRepository transactionRepository;

    @Override
    @Transactional
    public PaymentInitiationResponse initiatePayment(User user, double amount, String currency, String method) {
        String merchantOrderId = "CP-" + user.getId() + "-" + UUID.randomUUID();

        PaymentInitiationRequest req = new PaymentInitiationRequest(
                user.getId(), Math.round(amount * 100), currency, merchantOrderId, method);

        IPaymentProvider provider = providerResolver.resolve("PAYMOB");
        PaymentInitiationResult result = provider.initiate(req);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setStatus(PENDING);
        tx.setPaymentMethod(method);
        tx.setProvider(provider.getProviderName());
        tx.setMerchantOrderId(merchantOrderId);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

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

        PaymentTransaction tx = transactionRepository.findByMerchantOrderId(event.getMerchantOrderId())
                .orElseThrow(() -> new PaymentException.UnknownTransactionException(
                        "Unknown order: " + event.getMerchantOrderId()));

        if (tx.getStatus() != PENDING) {
            return; // idempotency guard against Paymob webhook retries
        }

        tx.setStatus(event.isSuccess() ? CONFIRMED : FAILED);
        tx.setProviderTransactionId(event.getProviderTransactionId());
        tx.setRawWebhookPayload(event.getRawPayload());
        if (!event.isSuccess()) {
            tx.setFailureReason("Declined by provider");
        }
        tx.setConfirmedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        if (event.isSuccess()) {
            // TODO: credit coin_wallets or activate subscription
        }
    }
}