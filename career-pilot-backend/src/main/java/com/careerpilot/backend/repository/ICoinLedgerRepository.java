package com.careerpilot.backend.repository;

import com.careerpilot.backend.entity.CoinLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ICoinLedgerRepository extends JpaRepository<CoinLedgerEntry, Long> {
    Page<CoinLedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);
}