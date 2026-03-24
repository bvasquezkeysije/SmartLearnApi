CREATE TABLE IF NOT EXISTS course_session_contents (
    id BIGSERIAL PRIMARY KEY,
    course_session_id BIGINT NOT NULL REFERENCES course_sessions(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    external_link TEXT NULL,
    file_name VARCHAR(255) NULL,
    file_data TEXT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_session_contents_session_deleted
    ON course_session_contents (course_session_id, deleted_at);
