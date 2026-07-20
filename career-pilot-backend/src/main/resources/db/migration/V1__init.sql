CREATE EXTENSION IF NOT EXISTS vector;

-- Users & Authentication
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    email VARCHAR(255) UNIQUE,
    phone_number VARCHAR(20) UNIQUE,
    verification_code VARCHAR(255),
    verification_code_expires_at TIMESTAMP,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    status BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Tracks
CREATE TABLE tracks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- User Profile (includes fields from V3 migration)
CREATE TABLE user_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    track_id BIGINT REFERENCES tracks(id),
    display_name VARCHAR(255),
    avatar_url VARCHAR(500),
    gender VARCHAR(10),
    date_of_birth DATE,
    target_role VARCHAR(255),
    industry VARCHAR(255),
    experience_level VARCHAR(20),
    current_job_title VARCHAR(255),
    years_of_experience INTEGER,
    cv_url VARCHAR(500),
    cv_filename VARCHAR(255),
    cv_uploaded_at TIMESTAMP,
    target_companies TEXT,
    education_level VARCHAR(100),
    timezone VARCHAR(50),
    terms_accepted BOOLEAN DEFAULT FALSE,
    subscription_tier VARCHAR(50),
    coin_balance INTEGER NOT NULL DEFAULT 0,
    onboarding_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- User Skills
CREATE TABLE user_skills (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    skill_name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    performance_score INTEGER DEFAULT 0,
    times_assessed INTEGER NOT NULL DEFAULT 0,
    last_assessed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_user_skill UNIQUE (user_id, skill_name)
);

CREATE INDEX idx_user_skills ON user_skills(user_id);

-- Question Bank
CREATE TABLE question_bank (
    id BIGSERIAL PRIMARY KEY,
    track_id BIGINT NOT NULL REFERENCES tracks(id),
    question_text TEXT NOT NULL,
    difficulty_level VARCHAR(50),
    category VARCHAR(100),
    expected_keywords TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_question_bank_track ON question_bank(track_id);

-- Interview Sessions
CREATE TABLE interview_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    track_id BIGINT NOT NULL REFERENCES tracks(id),
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    duration_seconds INTEGER,
    overall_score INTEGER,
    rag_context_ids TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_session_user ON interview_sessions(user_id, created_at DESC);

-- Session Questions
CREATE TABLE session_questions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES interview_sessions(id) ON DELETE CASCADE,
    question_id BIGINT REFERENCES question_bank(id),
    question_text TEXT NOT NULL,
    question_order INTEGER NOT NULL,
    user_transcript TEXT,
    audio_url VARCHAR(500),
    generated_by_llm BOOLEAN DEFAULT TRUE,
    coaching_tip TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP
);

-- Question Scores
CREATE TABLE question_scores (
    id BIGSERIAL PRIMARY KEY,
    session_question_id BIGINT NOT NULL UNIQUE REFERENCES session_questions(id) ON DELETE CASCADE,
    clarity INTEGER NOT NULL,
    confidence INTEGER NOT NULL,
    pacing INTEGER NOT NULL,
    filler_words INTEGER NOT NULL,
    content_relevance INTEGER NOT NULL,
    overall_question_score INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Feedback Reports
CREATE TABLE feedback_reports (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL UNIQUE REFERENCES interview_sessions(id) ON DELETE CASCADE,
    overall_score INTEGER NOT NULL,
    clarity_score INTEGER,
    confidence_score INTEGER,
    pacing_score INTEGER,
    filler_words_score INTEGER,
    content_relevance_score INTEGER,
    coaching_tips TEXT,
    generated_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Question History (RAG)
CREATE TABLE user_question_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    question_id BIGINT NOT NULL REFERENCES question_bank(id),
    session_id BIGINT REFERENCES interview_sessions(id),
    score_received INTEGER,
    asked_at TIMESTAMP NOT NULL,
    how_many_times_asked INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_question_history ON user_question_history(user_id, asked_at DESC);

-- RAG Context Documents
CREATE TABLE rag_context_documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    doc_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    vector vector(1536),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_rag_context_user ON rag_context_documents(user_id, doc_type);
CREATE INDEX idx_rag_context_vector ON rag_context_documents USING ivfflat (vector vector_cosine_ops);

-- Subscriptions
CREATE TABLE subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    tier VARCHAR(50) NOT NULL,
    renewal_date TIMESTAMP,
    started_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Coin Wallets
CREATE TABLE coin_wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    balance INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Payment Transactions (with V2 columns baked in)
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    wallet_id BIGINT REFERENCES coin_wallets(id),
    subscription_id BIGINT REFERENCES subscriptions(id),
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) DEFAULT 'USD',
    coin_pack_size INTEGER,
    tier_purchased VARCHAR(50),
    status VARCHAR(50),
    payment_method VARCHAR(50),
    merchant_order_id VARCHAR(255) UNIQUE,
    provider VARCHAR(50) NOT NULL DEFAULT 'PAYMOB',
    provider_transaction_id VARCHAR(255),
    failure_reason VARCHAR(500),
    raw_webhook_payload TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_payment_transactions_merchant_order_id ON payment_transactions(merchant_order_id);
CREATE INDEX idx_payment_transactions_provider ON payment_transactions(provider);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id, created_at DESC);

INSERT INTO roles (name, status) VALUES ('ROLE_USER', TRUE);
INSERT INTO roles (name, status) VALUES ('ROLE_ADMIN', TRUE);
