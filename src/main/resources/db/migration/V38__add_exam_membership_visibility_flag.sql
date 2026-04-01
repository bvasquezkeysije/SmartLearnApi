ALTER TABLE exam_memberships
    ADD COLUMN IF NOT EXISTS visible_in_exam_list BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE exam_memberships
SET visible_in_exam_list = TRUE
WHERE visible_in_exam_list IS NULL;
