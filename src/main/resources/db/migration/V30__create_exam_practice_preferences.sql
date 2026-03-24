CREATE TABLE IF NOT EXISTS exam_practice_preferences (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exam_id BIGINT NOT NULL REFERENCES exams(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    practice_feedback_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    practice_order_mode VARCHAR(20) NOT NULL DEFAULT 'ordered',
    practice_repeat_until_correct BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_practice_preferences_exam_user_active
    ON exam_practice_preferences (exam_id, user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_practice_preferences_user_active
    ON exam_practice_preferences (user_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_practice_preferences_exam_active
    ON exam_practice_preferences (exam_id)
    WHERE deleted_at IS NULL;
