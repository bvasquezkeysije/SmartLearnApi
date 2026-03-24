ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS visibility VARCHAR(16);

UPDATE courses
SET code = 'CURSO-' || LPAD(id::text, 6, '0')
WHERE code IS NULL OR BTRIM(code) = '';

UPDATE courses
SET visibility = 'public'
WHERE visibility IS NULL OR BTRIM(visibility) = '';

ALTER TABLE courses
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN visibility SET NOT NULL,
    ALTER COLUMN visibility SET DEFAULT 'public';

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_courses_visibility'
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT ck_courses_visibility CHECK (visibility IN ('public', 'private'));
    END IF;
END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_courses_code_active
    ON courses (LOWER(code))
    WHERE deleted_at IS NULL;
