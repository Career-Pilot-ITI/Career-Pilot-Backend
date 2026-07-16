--  Add word timings + pacing fields to session_questions

ALTER TABLE session_questions
    ADD COLUMN IF NOT EXISTS duration_ms        BIGINT,
    ADD COLUMN IF NOT EXISTS word_timings_json  TEXT,
    ADD COLUMN IF NOT EXISTS speech_rate_wpm    DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS avg_pause_ms       DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS silence_ratio      DOUBLE PRECISION;


CREATE INDEX IF NOT EXISTS idx_session_questions_completed
    ON session_questions(session_id, completed_at)
    WHERE completed_at IS NOT NULL;
