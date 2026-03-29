ALTER TABLE exam_group_sessions
    ADD COLUMN IF NOT EXISTS current_question_started_at TIMESTAMP NULL;

UPDATE exam_group_sessions
SET current_question_started_at = started_at
WHERE status = 'active'
  AND current_question_started_at IS NULL
  AND started_at IS NOT NULL;
