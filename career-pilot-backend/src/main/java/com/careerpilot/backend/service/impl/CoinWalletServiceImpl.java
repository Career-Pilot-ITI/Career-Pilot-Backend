package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.entity.CoinWallet;
import com.careerpilot.backend.entity.User;
import com.careerpilot.backend.repository.ICoinWalletRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoinWalletServiceImpl implements ICoinWalletService {

    private final ICoinWalletRepository walletRepository;

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
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId))
                .getBalance();
    }

    @Override
    @Transactional
    public void credit(Long userId, int amount) {
        CoinWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId));
        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void debit(Long userId, int amount) {
        CoinWallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new WalletException.WalletNotFoundException("No wallet found for user: " + userId));
        if (wallet.getBalance() < amount) {
            throw new WalletException.InsufficientBalanceException(
                    "Insufficient coin balance: have " + wallet.getBalance() + ", need " + amount);
        }
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);
    }
}