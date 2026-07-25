CREATE TABLE coin_ledger_entries (
                                     id BIGSERIAL PRIMARY KEY,
                                     wallet_id BIGINT NOT NULL REFERENCES coin_wallets(id),
                                     amount INTEGER NOT NULL,
                                     reason VARCHAR(50) NOT NULL,
                                     reference_id VARCHAR(255),
                                     created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_coin_ledger_wallet_id ON coin_ledger_entries(wallet_id);
CREATE INDEX idx_coin_ledger_created_at ON coin_ledger_entries(created_at);