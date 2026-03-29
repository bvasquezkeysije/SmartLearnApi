CREATE TABLE IF NOT EXISTS schedule_profiles (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    description VARCHAR(600) NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'private',
    reference_image_data TEXT NULL,
    reference_image_name VARCHAR(255) NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_schedule_profiles_owner_deleted
    ON schedule_profiles (owner_user_id, deleted_at, created_at DESC);

CREATE TABLE IF NOT EXISTS schedule_memberships (
    id BIGSERIAL PRIMARY KEY,
    schedule_profile_id BIGINT NOT NULL REFERENCES schedule_profiles(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'viewer',
    can_share BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_schedule_memberships_profile_user_active
    ON schedule_memberships (schedule_profile_id, user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_schedule_memberships_user_active
    ON schedule_memberships (user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_schedule_memberships_profile_active
    ON schedule_memberships (schedule_profile_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS schedule_activities (
    id BIGSERIAL PRIMARY KEY,
    schedule_profile_id BIGINT NOT NULL REFERENCES schedule_profiles(id) ON DELETE CASCADE,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(600) NULL,
    day_key VARCHAR(20) NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    location VARCHAR(180) NULL,
    color_key VARCHAR(20) NOT NULL DEFAULT 'blue',
    sort_order INTEGER NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_schedule_activities_profile_deleted
    ON schedule_activities (schedule_profile_id, deleted_at, created_at ASC);
