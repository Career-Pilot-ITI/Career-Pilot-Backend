-- Seed plan coins for existing users with an active subscription
-- (new users get their FREE coins via createWalletForUser on registration;
--  paid-tier coins are granted on each successful subscription payment)

UPDATE coin_wallets w
SET balance    = balance + CASE s.tier
                              WHEN 'FREE' THEN 20
                              WHEN 'PLUS' THEN 80
                              WHEN 'PRO' THEN 160
                              ELSE 0 END,
    updated_at = now()
FROM subscriptions s
WHERE s.user_id = w.user_id
  AND s.is_active = TRUE;

INSERT INTO coin_ledger_entries (wallet_id, amount, reason, reference_id, created_at)
SELECT w.id,
       CASE s.tier
           WHEN 'FREE' THEN 20
           WHEN 'PLUS' THEN 80
           WHEN 'PRO' THEN 160
           ELSE 0 END,
       'PLAN_GRANT',
       'SEED_' || w.id,
       now()
FROM coin_wallets w
         JOIN subscriptions s ON s.user_id = w.user_id
WHERE s.is_active = TRUE
  AND s.tier IN ('FREE', 'PLUS', 'PRO');
