package com.careerpilot.backend.service.impl;

import com.careerpilot.backend.controller.advice.SessionQuotaException;
import com.careerpilot.backend.controller.advice.WalletException;
import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;
import com.careerpilot.backend.entity.ENUMs.SubscriptionTier;
import com.careerpilot.backend.entity.Subscription;
import com.careerpilot.backend.repository.IInterviewSessionRepository;
import com.careerpilot.backend.repository.ISubscriptionRepository;
import com.careerpilot.backend.service.ICoinWalletService;
import com.careerpilot.backend.service.ISessionQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionQuotaServiceImpl implements ISessionQuotaService {

    private final ISubscriptionRepository subscriptionRepository;
    private final IInterviewSessionRepository sessionRepository;
    private final ICoinWalletService coinWalletService;

    @Override
    public boolean isPremium(Long userId) {
        return resolveSubscription(userId).getTier() != SubscriptionTier.FREE;
    }

    @Override
    @Transactional
    public void checkSessionQuota(Long userId, int sessionCost) {
        Subscription sub = resolveSubscription(userId);

        if (sub.getTier() == SubscriptionTier.PLUS || sub.getTier() == SubscriptionTier.PRO) return;

        if (!sub.getFreeTrialUsed()) {
            long totalSessions = sessionRepository.countByUserId(userId);
            if (totalSessions == 0) {
                sub.setFreeTrialUsed(true);
                subscriptionRepository.save(sub);
                return;
            }
        }

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        long monthlyCount = sessionRepository.countByUserIdAndCreatedAtAfter(userId, monthStart);

        if (monthlyCount >= 1) {
            try {
                coinWalletService.debit(userId, sessionCost, CoinLedgerReason.SESSION_SPEND, null);
            } catch (WalletException.InsufficientBalanceException e) {
                throw new SessionQuotaException.QuotaExceededException(
                        "You have 0 sessions remaining. Subscribe or buy coins.");
            }
        }
    }

    @Override
    @Transactional
    public boolean tryDebit(Long userId, int amount, CoinLedgerReason reason) {
        try {
            coinWalletService.debit(userId, amount, reason, null);
            return true;
        } catch (WalletException.InsufficientBalanceException e) {
            return false;
        }
    }

    private Subscription resolveSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.warn("No subscription found for user {} during quota check — treating as FREE tier", userId);
                    Subscription defaultFree = new Subscription();
                    defaultFree.setTier(SubscriptionTier.FREE);
                    defaultFree.setFreeTrialUsed(false);
                    return defaultFree;
                });
    }
}
