package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.CoinWallet;
import com.careerpilot.backend.entity.User;

public interface ICoinWalletService {
    CoinWallet createWalletForUser(User user);
    int getBalance(Long userId);
    void credit(Long userId, int amount);
    void debit(Long userId, int amount);
}