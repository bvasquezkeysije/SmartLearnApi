ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(20) NOT NULL DEFAULT 'private';

UPDATE exams
SET visibility = 'private'
WHERE visibility IS NULL;

CREATE TABLE IF NOT EXISTS exam_memberships (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL DEFAULT 'viewer',
    can_share BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_memberships_exam_user_active
    ON exam_memberships (exam_id, user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_memberships_user_active
    ON exam_memberships (user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_memberships_exam_active
    ON exam_memberships (exam_id)
    WHERE deleted_at IS NULL;
