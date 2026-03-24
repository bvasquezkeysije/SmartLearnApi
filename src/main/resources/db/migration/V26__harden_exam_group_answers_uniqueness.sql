-- Sanea posibles duplicados historicos por carrera de requests en entornos
-- donde no se aplico correctamente el indice unico previo.
WITH ranked_answers AS (
    SELECT
        ctid,
        ROW_NUMBER() OVER (
            PARTITION BY session_id, user_id, question_id
            ORDER BY
                CASE WHEN NULLIF(BTRIM(selected_answer), '') IS NULL THEN 0 ELSE 1 END DESC,
                answered_at DESC NULLS LAST,
                updated_at DESC NULLS LAST,
                id DESC
        ) AS row_rank
    FROM exam_group_session_answers
)
DELETE FROM exam_group_session_answers answers
USING ranked_answers ranked
WHERE answers.ctid = ranked.ctid
  AND ranked.row_rank > 1;

-- Asegura unicidad incluso si el indice historico no existe en la base actual.
CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_session_answers_unique_v26
    ON exam_group_session_answers (session_id, user_id, question_id);
