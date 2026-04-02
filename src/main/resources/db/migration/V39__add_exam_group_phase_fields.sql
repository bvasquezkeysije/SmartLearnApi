ALTER TABLE exam_group_sessions
    ADD COLUMN IF NOT EXISTS phase VARCHAR(20) NOT NULL DEFAULT 'open';

ALTER TABLE exam_group_sessions
    ADD COLUMN IF NOT EXISTS phase_started_at TIMESTAMP NULL;

ALTER TABLE exam_group_sessions
    ADD COLUMN IF NOT EXISTS phase_ends_at TIMESTAMP NULL;

ALTER TABLE exam_group_sessions
    ADD COLUMN IF NOT EXISTS question_version INTEGER NOT NULL DEFAULT 1;

UPDATE exam_group_sessions
SET phase = 'open'
WHERE phase IS NULL OR trim(phase) = '';

UPDATE exam_group_sessions
SET question_version = 1
WHERE question_version IS NULL OR question_version <= 0;

UPDATE exam_group_sessions
SET phase_started_at = COALESCE(current_question_started_at, started_at)
WHERE status = 'active'
  AND phase_started_at IS NULL;
