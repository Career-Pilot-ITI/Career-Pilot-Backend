package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IPaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByMerchantOrderId(String merchantOrderId);
}