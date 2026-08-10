ALTER TABLE job_listings
    ADD COLUMN company_logo_url VARCHAR(1024),
    ADD COLUMN posted_label     VARCHAR(100),
    ADD COLUMN applicants_label VARCHAR(100);