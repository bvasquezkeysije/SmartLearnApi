CREATE TABLE IF NOT EXISTS share_links (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL,
    expires_at TIMESTAMP NULL,
    max_claims INTEGER NULL,
    claims_count INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_share_links_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_share_links_token_active_deleted
    ON share_links (token, active, deleted_at);

CREATE INDEX IF NOT EXISTS idx_share_links_owner_deleted
    ON share_links (owner_user_id, deleted_at);

CREATE TABLE IF NOT EXISTS course_memberships (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL DEFAULT 'viewer',
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ux_course_memberships_course_user UNIQUE (course_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_course_memberships_user_deleted
    ON course_memberships (user_id, deleted_at);

CREATE INDEX IF NOT EXISTS idx_course_memberships_course_deleted
    ON course_memberships (course_id, deleted_at);
