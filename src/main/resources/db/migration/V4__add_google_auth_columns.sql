ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(32),
    ADD COLUMN IF NOT EXISTS google_subject VARCHAR(255),
    ADD COLUMN IF NOT EXISTS google_picture_url VARCHAR(500);

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_google_subject
    ON users (google_subject)
    WHERE google_subject IS NOT NULL;

UPDATE users
SET auth_provider = COALESCE(auth_provider, 'local');
