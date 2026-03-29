CREATE TABLE IF NOT EXISTS salas (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(180) NOT NULL,
    code VARCHAR(60) NOT NULL,
    visibility VARCHAR(20) NOT NULL DEFAULT 'public',
    description VARCHAR(800) NULL,
    image_data TEXT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_salas_code_active
    ON salas (LOWER(code))
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_salas_owner_deleted
    ON salas (owner_user_id, deleted_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_salas_visibility_deleted
    ON salas (visibility, deleted_at);

CREATE TABLE IF NOT EXISTS sala_memberships (
    id BIGSERIAL PRIMARY KEY,
    sala_id BIGINT NOT NULL REFERENCES salas(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'viewer',
    can_share BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sala_memberships_sala_user_active
    ON sala_memberships (sala_id, user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sala_memberships_user_active
    ON sala_memberships (user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_sala_memberships_sala_active
    ON sala_memberships (sala_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS sala_messages (
    id BIGSERIAL PRIMARY KEY,
    sala_id BIGINT NOT NULL REFERENCES salas(id) ON DELETE CASCADE,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sala_messages_sala_deleted
    ON sala_messages (sala_id, deleted_at, created_at ASC);
