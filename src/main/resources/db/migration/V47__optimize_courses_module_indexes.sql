-- Optimiza lecturas del modulo Cursos y listados de examenes para reducir N+1 y scans amplios.

CREATE INDEX IF NOT EXISTS idx_course_sessions_course_deleted_created
    ON course_sessions (course_id, deleted_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_course_weeks_session_deleted_order_created
    ON course_weeks (course_session_id, deleted_at, week_order, created_at);

CREATE INDEX IF NOT EXISTS idx_course_session_contents_session_deleted_order_created
    ON course_session_contents (course_session_id, deleted_at, content_order, created_at);

CREATE INDEX IF NOT EXISTS idx_course_memberships_course_deleted_created
    ON course_memberships (course_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_course_memberships_user_deleted_created
    ON course_memberships (user_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_course_competencies_course_deleted_sort_created
    ON course_competencies (course_id, deleted_at, sort_order, created_at);

CREATE INDEX IF NOT EXISTS idx_course_exams_course_created
    ON course_exams (course_id, created_at);

CREATE INDEX IF NOT EXISTS idx_exam_attempts_exam_user_created
    ON exam_attempts (exam_id, user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_exam_memberships_exam_deleted_created
    ON exam_memberships (exam_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_exam_memberships_user_deleted_created
    ON exam_memberships (user_id, deleted_at, created_at);

CREATE INDEX IF NOT EXISTS idx_exam_group_sessions_exam_deleted_status_created
    ON exam_group_sessions (exam_id, deleted_at, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_exam_group_sessions_exam_deleted_started_finished
    ON exam_group_sessions (exam_id, deleted_at, started_at, finished_at);
