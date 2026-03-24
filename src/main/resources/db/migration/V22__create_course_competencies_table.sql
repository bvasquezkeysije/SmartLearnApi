CREATE TABLE IF NOT EXISTS course_competencies (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    level VARCHAR(20) NOT NULL DEFAULT 'basico',
    sort_order INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_competencies_course_deleted
    ON course_competencies (course_id, deleted_at, sort_order, created_at);

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_course_competencies_level'
    ) THEN
        ALTER TABLE course_competencies
            ADD CONSTRAINT ck_course_competencies_level CHECK (level IN ('basico', 'intermedio', 'avanzado'));
    END IF;
END
$$;

