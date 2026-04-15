-- V50 (Fase A - expand): base estructural para motor de rondas en repaso grupal.
-- Esta migracion NO rompe compatibilidad con el modelo actual.

CREATE TABLE IF NOT EXISTS exam_group_session_rounds (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NOT NULL,
    round_number INTEGER NOT NULL,
    question_id BIGINT NOT NULL,
    phase VARCHAR(16) NOT NULL DEFAULT 'open',
    open_started_at TIMESTAMP,
    open_ends_at TIMESTAMP,
    review_started_at TIMESTAMP,
    review_ends_at TIMESTAMP,
    close_reason VARCHAR(32),
    deleted_at TIMESTAMP,
    CONSTRAINT fk_exam_group_rounds_session
        FOREIGN KEY (session_id) REFERENCES exam_group_sessions(id),
    CONSTRAINT fk_exam_group_rounds_question
        FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT uq_exam_group_rounds_session_round
        UNIQUE (session_id, round_number),
    CONSTRAINT ck_exam_group_rounds_phase
        CHECK (phase IN ('open', 'review', 'closed')),
    CONSTRAINT ck_exam_group_rounds_close_reason
        CHECK (
            close_reason IS NULL
            OR close_reason IN ('all_answered', 'timer_expired', 'manual_next', 'session_closed')
        ),
    CONSTRAINT ck_exam_group_rounds_open_window
        CHECK (open_started_at IS NULL OR open_ends_at IS NULL OR open_ends_at >= open_started_at),
    CONSTRAINT ck_exam_group_rounds_review_window
        CHECK (review_started_at IS NULL OR review_ends_at IS NULL OR review_ends_at >= review_started_at)
);

CREATE INDEX IF NOT EXISTS idx_exam_group_rounds_session_active
    ON exam_group_session_rounds (session_id, round_number)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_rounds_phase_active
    ON exam_group_session_rounds (session_id, phase, review_ends_at, open_ends_at)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS exam_group_session_events (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    session_id BIGINT NOT NULL,
    round_number INTEGER,
    actor_user_id BIGINT,
    event_type VARCHAR(64) NOT NULL,
    payload_json TEXT,
    CONSTRAINT fk_exam_group_events_session
        FOREIGN KEY (session_id) REFERENCES exam_group_sessions(id),
    CONSTRAINT fk_exam_group_events_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_exam_group_events_session_time
    ON exam_group_session_events (session_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_exam_group_events_type_time
    ON exam_group_session_events (event_type, created_at DESC);

ALTER TABLE exam_group_session_answers
    ADD COLUMN IF NOT EXISTS round_number INTEGER,
    ADD COLUMN IF NOT EXISTS question_version INTEGER,
    ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_final BOOLEAN;

UPDATE exam_group_session_answers
SET round_number = 1
WHERE round_number IS NULL;

UPDATE exam_group_session_answers
SET question_version = 1
WHERE question_version IS NULL;

UPDATE exam_group_session_answers
SET submitted_at = COALESCE(answered_at, created_at, CURRENT_TIMESTAMP)
WHERE submitted_at IS NULL;

UPDATE exam_group_session_answers
SET is_final = TRUE
WHERE is_final IS NULL;

ALTER TABLE exam_group_session_answers
    ALTER COLUMN round_number SET DEFAULT 1,
    ALTER COLUMN round_number SET NOT NULL,
    ALTER COLUMN question_version SET DEFAULT 1,
    ALTER COLUMN question_version SET NOT NULL,
    ALTER COLUMN submitted_at SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN submitted_at SET NOT NULL,
    ALTER COLUMN is_final SET DEFAULT TRUE,
    ALTER COLUMN is_final SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_answers_session_round_user
    ON exam_group_session_answers (session_id, round_number, user_id);

CREATE INDEX IF NOT EXISTS idx_exam_group_answers_round_question
    ON exam_group_session_answers (session_id, round_number, question_id, submitted_at);

