CREATE TABLE IF NOT EXISTS course_weeks (
    id BIGSERIAL PRIMARY KEY,
    course_session_id BIGINT NOT NULL REFERENCES course_sessions(id) ON DELETE CASCADE,
    week_order INTEGER NOT NULL DEFAULT 1,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_course_weeks_session_order
    ON course_weeks (course_session_id, week_order);

CREATE INDEX IF NOT EXISTS idx_course_weeks_session_deleted
    ON course_weeks (course_session_id, deleted_at);

ALTER TABLE course_session_contents
    ADD COLUMN IF NOT EXISTS course_week_id BIGINT NULL REFERENCES course_weeks(id) ON DELETE CASCADE;

CREATE INDEX IF NOT EXISTS idx_course_session_contents_week_deleted
    ON course_session_contents (course_week_id, deleted_at);

INSERT INTO course_weeks (
    course_session_id,
    week_order,
    name,
    description,
    deleted_at,
    created_at,
    updated_at
)
SELECT
    session.id,
    1,
    'SEMANA 1: Inicio',
    session.weekly_content,
    session.deleted_at,
    COALESCE(session.created_at, CURRENT_TIMESTAMP),
    COALESCE(session.updated_at, CURRENT_TIMESTAMP)
FROM course_sessions session
ON CONFLICT (course_session_id, week_order) DO NOTHING;

UPDATE course_session_contents content
SET course_week_id = week.id
FROM course_weeks week
WHERE week.course_session_id = content.course_session_id
  AND week.week_order = 1
  AND content.course_week_id IS NULL;

ALTER TABLE course_session_contents
    ALTER COLUMN course_week_id SET NOT NULL;
