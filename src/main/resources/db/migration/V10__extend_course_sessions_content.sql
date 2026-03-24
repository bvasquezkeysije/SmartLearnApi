ALTER TABLE course_sessions
    ADD COLUMN IF NOT EXISTS links_text TEXT,
    ADD COLUMN IF NOT EXISTS material_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS material_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS material_file_data TEXT,
    ADD COLUMN IF NOT EXISTS cloned_exam_id BIGINT NULL REFERENCES exams(id);

CREATE INDEX IF NOT EXISTS idx_course_sessions_cloned_exam
    ON course_sessions (cloned_exam_id);
