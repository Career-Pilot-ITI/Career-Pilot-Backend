CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       password VARCHAR(255),
                       email VARCHAR(255),
                       phone_number VARCHAR(20),
                       verification_code VARCHAR(255),
                       verification_code_expires_at TIMESTAMP,
                       enabled BOOLEAN NOT NULL DEFAULT FALSE,
                       CONSTRAINT uk_users_email UNIQUE (email),
                       CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
                       CONSTRAINT uk_users_username UNIQUE (username)
);

CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       status BOOLEAN NOT NULL DEFAULT TRUE,
                       CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE user_roles (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            status BOOLEAN NOT NULL DEFAULT TRUE,
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

INSERT INTO roles (name, status) VALUES ('ROLE_USER', TRUE);

CREATE TABLE tracks (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL UNIQUE,
                        description TEXT,
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP
);

CREATE TABLE user_profiles (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
                               track_id BIGINT NOT NULL REFERENCES tracks(id),
                               cv_url VARCHAR(500),
                               cv_filename VARCHAR(255),
                               cv_uploaded_at TIMESTAMP,
                               subscription_tier VARCHAR(50),
                               coin_balance INTEGER NOT NULL DEFAULT 0,
                               onboarding_completed BOOLEAN DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP
);

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

CREATE TABLE session_questions (
                                   id BIGSERIAL PRIMARY KEY,
                                   session_id BIGINT NOT NULL REFERENCES interview_sessions(id),
                                   question_id BIGINT REFERENCES question_bank(id),
                                   question_text TEXT NOT NULL,
                                   question_order INTEGER NOT NULL,
                                   user_transcript TEXT,
                                   audio_url VARCHAR(500),
                                   generated_by_llm BOOLEAN DEFAULT TRUE,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   completed_at TIMESTAMP
);

CREATE TABLE question_scores (
                                 id BIGSERIAL PRIMARY KEY,
                                 session_question_id BIGINT NOT NULL UNIQUE REFERENCES session_questions(id),
                                 clarity INTEGER NOT NULL,
                                 confidence INTEGER NOT NULL,
                                 pacing INTEGER NOT NULL,
                                 filler_words INTEGER NOT NULL,
                                 content_relevance INTEGER NOT NULL,
                                 overall_question_score INTEGER NOT NULL,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE feedback_reports (
                                  id BIGSERIAL PRIMARY KEY,
                                  session_id BIGINT NOT NULL UNIQUE REFERENCES interview_sessions(id),
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

CREATE TABLE question_bank (
                               id BIGSERIAL PRIMARY KEY,
                               track_id BIGINT NOT NULL REFERENCES tracks(id),
                               question_text TEXT NOT NULL,
                               difficulty_level VARCHAR(50),
                               category VARCHAR(100),
                               expected_keywords TEXT,
                               is_active BOOLEAN NOT NULL DEFAULT TRUE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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

CREATE TABLE coin_wallets (
                              id BIGSERIAL PRIMARY KEY,
                              user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
                              balance INTEGER NOT NULL DEFAULT 0,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP
);


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
                                      paymob_transaction_id VARCHAR(255),
                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      confirmed_at TIMESTAMP
);


CREATE TABLE otp_logs (
                          id BIGSERIAL PRIMARY KEY,
                          phone_number VARCHAR(20) NOT NULL,
                          otp_hash VARCHAR(255) NOT NULL,
                          attempt_count INTEGER NOT NULL DEFAULT 0,
                          lockout_until TIMESTAMP,
                          status VARCHAR(50),
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          expires_at TIMESTAMP NOT NULL,
                          verified_at TIMESTAMP
);


CREATE INDEX idx_user_question_history ON user_question_history(user_id, asked_at DESC);
CREATE INDEX idx_rag_context_user ON rag_context_documents(user_id, doc_type);
CREATE INDEX idx_rag_context_vector ON rag_context_documents USING ivfflat (vector vector_cosine_ops);
CREATE INDEX idx_session_user ON interview_sessions(user_id, created_at DESC);
CREATE INDEX idx_question_bank_track ON question_bank(track_id);