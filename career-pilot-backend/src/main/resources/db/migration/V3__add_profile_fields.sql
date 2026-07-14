ALTER TABLE user_profiles
    ADD COLUMN display_name VARCHAR(255),
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN gender VARCHAR(10),
    ADD COLUMN date_of_birth DATE,
    ADD COLUMN target_role VARCHAR(255),
    ADD COLUMN industry VARCHAR(255),
    ADD COLUMN experience_level VARCHAR(20),
    ADD COLUMN current_job_title VARCHAR(255),
    ADD COLUMN years_of_experience INTEGER,
    ADD COLUMN target_companies TEXT,
    ADD COLUMN education_level VARCHAR(20),
    ADD COLUMN timezone VARCHAR(50),
    ADD COLUMN terms_accepted BOOLEAN DEFAULT FALSE;

ALTER TABLE user_profiles ALTER COLUMN track_id DROP NOT NULL;
