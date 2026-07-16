package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.PaymentTransaction;
import com.careerpilot.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IPaymentTransactionService {
    PaymentTransaction createPending(User user, double amount, String currency, String method,
                                     String providerName, String merchantOrderId,
                                     Integer coinPackSize, String tier);

    Optional<PaymentTransaction> findByMerchantOrderId(String merchantOrderId);

    PaymentTransaction markConfirmed(PaymentTransaction tx, String providerTransactionId, String rawPayload);

    PaymentTransaction markFailed(PaymentTransaction tx, String providerTransactionId, String rawPayload, String failureReason);

    Page<PaymentTransaction> findByUser(Long userId, Pageable pageable);
}