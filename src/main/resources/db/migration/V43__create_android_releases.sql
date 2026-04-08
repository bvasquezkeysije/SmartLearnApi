CREATE TABLE IF NOT EXISTS android_releases (
    id BIGSERIAL PRIMARY KEY,
    version_name VARCHAR(64) NOT NULL,
    version_code INTEGER NOT NULL,
    apk_url VARCHAR(2000) NOT NULL,
    checksum_sha256 VARCHAR(128) NULL,
    release_notes TEXT NULL,
    is_active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_android_releases_active_true
ON android_releases (is_active)
WHERE is_active = TRUE;

CREATE INDEX IF NOT EXISTS idx_android_releases_version_code
ON android_releases (version_code DESC);
