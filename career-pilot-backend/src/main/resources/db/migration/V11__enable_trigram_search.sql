-- V11: lexical search support via pg_trgm.
-- Backs the word_similarity() ranking used as the fallback when the vector
-- store is empty or an embedding match is below threshold.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_tracks_name_trgm
    ON tracks USING gin (name gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_tracks_description_trgm
    ON tracks USING gin (description gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_question_bank_text_trgm
    ON question_bank USING gin (question_text gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_question_bank_keywords_trgm
    ON question_bank USING gin (expected_keywords gin_trgm_ops);
