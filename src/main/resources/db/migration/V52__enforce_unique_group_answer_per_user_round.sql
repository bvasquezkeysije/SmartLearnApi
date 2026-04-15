-- Consolidar duplicados antes de imponer unicidad por ronda.
WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY session_id, user_id, round_number
            ORDER BY submitted_at DESC NULLS LAST, answered_at DESC NULLS LAST, id DESC
        ) AS rn
    FROM exam_group_session_answers
)
DELETE FROM exam_group_session_answers a
USING ranked r
WHERE a.id = r.id
  AND r.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_session_answers_session_user_round
    ON exam_group_session_answers (session_id, user_id, round_number);

