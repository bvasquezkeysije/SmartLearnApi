ALTER TABLE exam_memberships
    ADD COLUMN IF NOT EXISTS can_start_group BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE exam_memberships
SET can_start_group = FALSE
WHERE can_start_group IS NULL;

CREATE TABLE IF NOT EXISTS exam_group_sessions (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    created_by_user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'waiting',
    order_mode VARCHAR(16) NOT NULL DEFAULT 'ordered',
    question_ids TEXT NULL,
    total_questions INTEGER NOT NULL DEFAULT 0,
    current_question_index INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_exam_group_sessions_exam_active
    ON exam_group_sessions (exam_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS exam_group_session_members (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NOT NULL REFERENCES exam_group_sessions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    connected BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_session_members_active
    ON exam_group_session_members (session_id, user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_session_members_session_active
    ON exam_group_session_members (session_id)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS exam_group_session_answers (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NOT NULL REFERENCES exam_group_sessions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    question_id BIGINT NOT NULL REFERENCES questions(id),
    selected_answer TEXT NULL,
    is_correct BOOLEAN NULL,
    answered_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_session_answers_unique
    ON exam_group_session_answers (session_id, user_id, question_id);

CREATE INDEX IF NOT EXISTS idx_exam_group_session_answers_question
    ON exam_group_session_answers (session_id, question_id);
