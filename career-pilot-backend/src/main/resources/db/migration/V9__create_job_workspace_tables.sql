-- single job can be cached to multiple users and created by a single user
-- which means the user id is not reference in constraint of access but more like ownership of the scrapping 
-- same job will not be processed multiple times only once
CREATE TABLE job_listings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    location VARCHAR(255),
    description TEXT,
    employment_type VARCHAR(50),
    seniority_level VARCHAR(50),
    required_skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    preferred_skills JSONB NOT NULL DEFAULT '[]'::jsonb,
    responsibilities TEXT,
    qualifications TEXT,
    technologies JSONB NOT NULL DEFAULT '[]'::jsonb,
    salary_min INTEGER,
    salary_max INTEGER,
    currency VARCHAR(10),
    experience_years INTEGER,
    education_level VARCHAR(100),
    application_url VARCHAR(500),
    source_url VARCHAR(500),
    source_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_job_listings_source_url ON job_listings(source_url) WHERE source_url IS NOT NULL;
CREATE INDEX idx_job_listings_user_id ON job_listings(user_id);

CREATE TABLE job_workspaces (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    job_id BIGINT NOT NULL REFERENCES job_listings(id),
    status VARCHAR(20) NOT NULL DEFAULT 'SAVED',
    cv_score INTEGER,
    cv_score_updated_at TIMESTAMP,
    cv_optimized_text TEXT,
    cover_letter_text TEXT,
    last_interview_session_id BIGINT REFERENCES interview_sessions(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX uq_job_workspaces_user_job ON job_workspaces(user_id, job_id);
CREATE INDEX idx_job_workspaces_user_id ON job_workspaces(user_id);
CREATE INDEX idx_job_workspaces_job_id ON job_workspaces(job_id);
