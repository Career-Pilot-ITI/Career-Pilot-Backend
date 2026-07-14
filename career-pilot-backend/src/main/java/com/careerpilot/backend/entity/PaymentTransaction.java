package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.PaymentStatus;
import com.careerpilot.backend.entity.ENUMs.PaymentProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private CoinWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "currency", nullable = false)
    private String currency = "USD";

    @Column(name = "coin_pack_size")
    private Integer coinPackSize;

    @Column(name = "tier_purchased")
    private String tierPurchased;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;     // PENDING, CONFIRMED, FAILED, REFUNDED

    @Column(name = "payment_method")
    private String paymentMethod;     // "PAYMOB", "STOREKIT"

    @Column(name = "merchant_order_id", unique = true)
    private String merchantOrderId;
    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentProvider provider = PaymentProvider.PAYMOB;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "raw_webhook_payload", columnDefinition = "TEXT")
    private String rawWebhookPayload;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}