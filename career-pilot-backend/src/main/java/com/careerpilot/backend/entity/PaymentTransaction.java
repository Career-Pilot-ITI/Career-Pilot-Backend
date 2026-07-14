package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.PaymentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private CoinWallet wallet;        // nullable (for subscription)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription; // nullable (for coins)

    @Column(name = "amount", nullable = false)
    private Double amount;            // 9.99, 49.99, etc.

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Column(name = "coin_pack_size")
    private Integer coinPackSize;     // 100, 500, 1000 (nullable if subscription)

    @Column(name = "tier_purchased")
    private String tierPurchased;     // "PLUS", "PRO" (nullable if coins)

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;     // PENDING, CONFIRMED, FAILED, REFUNDED

    @Column(name = "payment_method")
    private String paymentMethod;     // "PAYMOB", "STOREKIT"

    @Column(name = "paymob_transaction_id")
    private String paymobTransactionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}


