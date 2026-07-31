package com.careerpilot.backend.service;

import com.careerpilot.backend.entity.ENUMs.CoinLedgerReason;

/**
 * Shared entitlement checks. Encodes "is this user premium, or can they pay?"
 * so interview sessions, job parsing, and other coin-gated features follow one
 * rule: PLUS/PRO pass free, everyone else pays coins or gets the cheap path.
 */
public interface ISessionQuotaService {

    /**
     * True when the user's subscription tier is PLUS or PRO.
     */
    boolean isPremium(Long userId);

    /**
     * Interview-specific quota: PLUS/PRO pass free, free-trial first session
     * is free, otherwise the monthly free session then coin debit. Throws
     * {@link com.careerpilot.backend.controller.advice.SessionQuotaException.QuotaExceededException}
     * when the user has no coins left.
     */
    void checkSessionQuota(Long userId, int sessionCost);

    /**
     * Attempts to debit {@code amount} coins for {@code reason}. Returns true on
     * success, false when the balance is insufficient (no exception).
     */
    boolean tryDebit(Long userId, int amount, CoinLedgerReason reason);
}
