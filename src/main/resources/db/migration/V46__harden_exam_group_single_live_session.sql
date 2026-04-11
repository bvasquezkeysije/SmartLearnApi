-- Garantiza consistencia: solo una sesion viva (waiting/active) por examen.
-- Antes de crear el índice único parcial, cerramos duplicados históricos dejando
-- solo la sesión más reciente por examen.
WITH live_ranked AS (
    SELECT
        id,
        exam_id,
        ROW_NUMBER() OVER (PARTITION BY exam_id ORDER BY created_at DESC, id DESC) AS rn
    FROM exam_group_sessions
    WHERE deleted_at IS NULL
      AND status IN ('waiting', 'active')
)
UPDATE exam_group_sessions s
SET
    status = 'finished',
    finished_at = COALESCE(s.finished_at, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
FROM live_ranked lr
WHERE s.id = lr.id
  AND lr.rn > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_sessions_single_live
    ON exam_group_sessions (exam_id)
    WHERE deleted_at IS NULL
      AND status IN ('waiting', 'active');

