-- V12__create_ai_jobs.sql
CREATE TABLE ai_jobs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    workspace_id BIGINT NOT NULL REFERENCES job_workspaces(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,            -- e.g., 'CV_OPTIMIZE'
    status VARCHAR(50) NOT NULL,          -- PENDING, PROCESSING, COMPLETED, FAILED
    progress_percentage INT NOT NULL DEFAULT 0,
    current_step VARCHAR(255),            -- e.g., 'Analyzing summary...', 'Optimizing experience...'
    result JSONB,                         -- Stores final CvOptimizationResponse JSON
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_jobs_user_id ON ai_jobs(user_id);
CREATE INDEX idx_ai_jobs_workspace_id_type ON ai_jobs(workspace_id, type);
