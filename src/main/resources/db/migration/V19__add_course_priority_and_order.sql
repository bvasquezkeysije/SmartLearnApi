ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS priority VARCHAR(24),
    ADD COLUMN IF NOT EXISTS sort_order INTEGER;

UPDATE courses
SET priority = 'important'
WHERE priority IS NULL OR BTRIM(priority) = '';

UPDATE courses
SET sort_order = 0
WHERE sort_order IS NULL OR sort_order < 0;

ALTER TABLE courses
    ALTER COLUMN priority SET NOT NULL,
    ALTER COLUMN sort_order SET NOT NULL,
    ALTER COLUMN priority SET DEFAULT 'important',
    ALTER COLUMN sort_order SET DEFAULT 0;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_courses_priority'
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT ck_courses_priority CHECK (priority IN ('very_important', 'important', 'low_important', 'optional'));
    END IF;
END
$$;

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_courses_sort_order'
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT ck_courses_sort_order CHECK (sort_order >= 0);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_courses_priority_sort_order
    ON courses (priority, sort_order, created_at DESC);
