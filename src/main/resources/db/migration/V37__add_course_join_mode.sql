ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS join_mode VARCHAR(16);

UPDATE courses
SET join_mode = 'open'
WHERE join_mode IS NULL OR BTRIM(join_mode) = '';

ALTER TABLE courses
    ALTER COLUMN join_mode SET NOT NULL,
    ALTER COLUMN join_mode SET DEFAULT 'open';

DO
$$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_courses_join_mode'
    ) THEN
        ALTER TABLE courses
            ADD CONSTRAINT ck_courses_join_mode CHECK (join_mode IN ('open', 'request'));
    END IF;
END
$$;
