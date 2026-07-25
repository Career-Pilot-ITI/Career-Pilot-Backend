package com.careerpilot.backend.entity;

import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "coin_ledger_entries")
@Getter
@Setter
public class CoinLedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private CoinWallet wallet;

    @Column(name = "amount", nullable = false)
    private Integer amount; // positive = credit, negative = debit

    @Column(name = "reason", nullable = false)
    @Enumerated(EnumType.STRING)
    private CoinLedgerReason reason;

    @Column(name = "reference_id")
    private String referenceId; // merchant_order_id (top-up) or session id (spend)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}