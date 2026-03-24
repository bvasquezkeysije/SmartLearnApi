ALTER TABLE course_session_contents
    ADD COLUMN IF NOT EXISTS source_exam_id BIGINT NULL REFERENCES exams(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_course_session_contents_source_exam
    ON course_session_contents (source_exam_id);
