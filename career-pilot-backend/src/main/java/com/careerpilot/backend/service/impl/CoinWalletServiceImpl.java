package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.entity.CoinLedgerEntry;
import com.careerpilot.backend.entity.CoinWallet;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.ICoinLedgerRepository;
import com.careerpilot.backend.repository.ICoinWalletRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoinWalletServiceImpl implements ICoinWalletService {

    private final ICoinWalletRepository walletRepository;
    private final ICoinLedgerRepository ledgerRepository;

    @Override
    @Transactional
    public CoinWallet createWalletForUser(User user) {
        CoinWallet wallet = new CoinWallet();
        wallet.setUser(user);
        wallet.setBalance(0);
        return walletRepository.save(wallet);
    }

    @Override
    public int getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(CoinWallet::getBalance)
                .orElse(0);
    }

    @Override
    @Transactional
    public void credit(Long userId, int amount, CoinLedgerReason reason, String referenceId) {
        CoinWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId));
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
        writeLedgerEntry(wallet, amount, reason, referenceId);
    }

    @Override
    @Transactional
    public void debit(Long userId, int amount, CoinLedgerReason reason, String referenceId) {
        CoinWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId));
        if (wallet.getBalance() < amount) {
            throw new WalletException.InsufficientBalanceException(
                    "Insufficient coin balance: have " + wallet.getBalance() + ", need " + amount);
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
        writeLedgerEntry(wallet, -amount, reason, referenceId);
    }

    @Override
    public Page<CoinLedgerEntry> getLedgerHistory(Long userId, Pageable pageable) {
        CoinWallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId));
        return ledgerRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
    }

    private void writeLedgerEntry(CoinWallet wallet, int signedAmount, CoinLedgerReason reason, String referenceId) {
        CoinLedgerEntry entry = new CoinLedgerEntry();
        entry.setWallet(wallet);
        entry.setAmount(signedAmount);
        entry.setReason(reason);
        entry.setReferenceId(referenceId);
        ledgerRepository.save(entry);
    }
}