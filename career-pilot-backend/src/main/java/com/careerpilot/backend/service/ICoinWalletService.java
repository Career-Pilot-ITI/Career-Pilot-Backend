package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.CoinLedgerEntry;
import com.careerpilot.backend.entity.CoinWallet;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICoinWalletService {
    CoinWallet createWalletForUser(User user);
    int getBalance(Long userId);
    void credit(Long userId, int amount, CoinLedgerReason reason, String referenceId);
    void debit(Long userId, int amount, CoinLedgerReason reason, String referenceId);
    Page<CoinLedgerEntry> getLedgerHistory(Long userId, Pageable pageable);
}