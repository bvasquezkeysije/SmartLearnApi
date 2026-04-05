DROP INDEX IF EXISTS ux_course_weeks_session_order;

CREATE UNIQUE INDEX IF NOT EXISTS ux_course_weeks_session_order
    ON course_weeks (course_session_id, week_order)
    WHERE deleted_at IS NULL;
