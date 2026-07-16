package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.entity.ENUMs.PaymentStatus;
import com.careerpilot.backend.entity.PaymentTransaction;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.IPaymentTransactionRepository;
import com.careerpilot.backend.service.IPaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentTransactionServiceImpl implements IPaymentTransactionService {

    private final IPaymentTransactionRepository transactionRepository;

    @Override
    public PaymentTransaction createPending(User user, double amount, String currency, String method,
                                            String providerName, String merchantOrderId,
                                            Integer coinPackSize, String tier) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setUser(user);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setStatus(PaymentStatus.PENDING);
        tx.setPaymentMethod(method);
        tx.setProvider(providerName);
        tx.setMerchantOrderId(merchantOrderId);
        tx.setCoinPackSize(coinPackSize);
        tx.setTierPurchased(tier);
        tx.setCreatedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Override
    public Optional<PaymentTransaction> findByMerchantOrderId(String merchantOrderId) {
        return transactionRepository.findByMerchantOrderId(merchantOrderId);
    }

    @Override
    public PaymentTransaction markConfirmed(PaymentTransaction tx, String providerTransactionId, String rawPayload) {
        tx.setStatus(PaymentStatus.CONFIRMED);
        tx.setProviderTransactionId(providerTransactionId);
        tx.setRawWebhookPayload(rawPayload);
        tx.setConfirmedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Override
    public PaymentTransaction markFailed(PaymentTransaction tx, String providerTransactionId, String rawPayload, String failureReason) {
        tx.setStatus(PaymentStatus.FAILED);
        tx.setProviderTransactionId(providerTransactionId);
        tx.setRawWebhookPayload(rawPayload);
        tx.setFailureReason(failureReason);
        tx.setConfirmedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @Override
    public Page<PaymentTransaction> findByUser(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable);
    }
}