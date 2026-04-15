CREATE TABLE IF NOT EXISTS exam_group_room_sessions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    room_token VARCHAR(128) NOT NULL,
    issued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

ALTER TABLE exam_group_room_sessions
    ADD CONSTRAINT exam_group_room_sessions_session_id_fkey
        FOREIGN KEY (session_id) REFERENCES exam_group_sessions(id);

ALTER TABLE exam_group_room_sessions
    ADD CONSTRAINT exam_group_room_sessions_user_id_fkey
        FOREIGN KEY (user_id) REFERENCES users(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_room_sessions_room_token
    ON exam_group_room_sessions (room_token)
    WHERE deleted_at IS NULL AND revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_room_sessions_session_user_active
    ON exam_group_room_sessions (session_id, user_id, expires_at)
    WHERE deleted_at IS NULL AND revoked_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_room_sessions_session_token_active
    ON exam_group_room_sessions (session_id, room_token)
    WHERE deleted_at IS NULL AND revoked_at IS NULL;

-- Backfill opcional desde V48 si existieran tokens en membresias.
INSERT INTO exam_group_room_sessions (session_id, user_id, room_token, issued_at, expires_at, created_at, updated_at)
SELECT
    m.session_id,
    m.user_id,
    m.room_session_token,
    COALESCE(m.room_session_issued_at, NOW()),
    COALESCE(m.room_session_expires_at, NOW() + INTERVAL '180 minutes'),
    COALESCE(m.created_at, NOW()),
    COALESCE(m.updated_at, NOW())
FROM exam_group_session_members m
WHERE m.deleted_at IS NULL
  AND m.room_session_token IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM exam_group_room_sessions rs
      WHERE rs.room_token = m.room_session_token
        AND rs.deleted_at IS NULL
        AND rs.revoked_at IS NULL
  );

