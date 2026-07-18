ALTER TABLE interview_sessions
ADD COLUMN updated_at TIMESTAMP;

CREATE INDEX idx_session_updated_at ON interview_sessions(updated_at);
