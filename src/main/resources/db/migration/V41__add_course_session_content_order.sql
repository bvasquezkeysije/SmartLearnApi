ALTER TABLE course_session_contents
    ADD COLUMN IF NOT EXISTS content_order INTEGER;

WITH ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY course_week_id
               ORDER BY COALESCE(created_at, NOW()), id
           ) AS next_order
    FROM course_session_contents
    WHERE deleted_at IS NULL
)
UPDATE course_session_contents c
SET content_order = ranked.next_order
FROM ranked
WHERE c.id = ranked.id
  AND (c.content_order IS NULL OR c.content_order < 1);

UPDATE course_session_contents
SET content_order = 1
WHERE content_order IS NULL OR content_order < 1;

ALTER TABLE course_session_contents
    ALTER COLUMN content_order SET DEFAULT 1;

ALTER TABLE course_session_contents
    ALTER COLUMN content_order SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_course_session_contents_week_order
    ON course_session_contents(course_week_id, content_order, created_at, id);
