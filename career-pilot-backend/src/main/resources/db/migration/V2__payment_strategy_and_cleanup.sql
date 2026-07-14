-- 1. Add payment strategy columns
ALTER TABLE payment_transactions
    ADD COLUMN merchant_order_id VARCHAR(255) UNIQUE,
    ADD COLUMN provider VARCHAR(50) NOT NULL DEFAULT 'PAYMOB',
    ADD COLUMN failure_reason VARCHAR(500),
    ADD COLUMN raw_webhook_payload TEXT;

-- 2. Rename column for provider abstraction
ALTER TABLE payment_transactions
    RENAME COLUMN paymob_transaction_id TO provider_transaction_id;

-- 3. Add constraint for merchant order ID uniqueness
ALTER TABLE payment_transactions
    ADD CONSTRAINT uk_payment_transactions_merchant_order_id
        UNIQUE (merchant_order_id);

-- 4. Add indexes for payment lookups
CREATE INDEX idx_payment_transactions_merchant_order_id
    ON payment_transactions(merchant_order_id);

CREATE INDEX idx_payment_transactions_provider
    ON payment_transactions(provider);

CREATE INDEX idx_payment_transactions_status
    ON payment_transactions(status);

CREATE INDEX idx_payment_transactions_user_id
    ON payment_transactions(user_id, created_at DESC);

-- 5. Remove OtpLog table (not needed for MVP)
DROP TABLE IF EXISTS otp_logs;