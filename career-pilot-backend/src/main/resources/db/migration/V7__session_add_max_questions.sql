ALTER TABLE interview_sessions 
    ADD COLUMN max_questions INTEGER NOT NULL DEFAULT 10,
    ADD COLUMN target_duration_minutes INTEGER NOT NULL DEFAULT 15;
