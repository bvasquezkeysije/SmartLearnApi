ALTER TABLE android_releases
    ADD COLUMN IF NOT EXISTS file_name VARCHAR(255) NULL,
    ADD COLUMN IF NOT EXISTS file_size_bytes BIGINT NULL,
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(120) NULL,
    ADD COLUMN IF NOT EXISTS storage_key VARCHAR(255) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_android_releases_storage_key
ON android_releases(storage_key)
WHERE storage_key IS NOT NULL;
