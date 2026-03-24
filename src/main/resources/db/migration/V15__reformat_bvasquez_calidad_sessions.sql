WITH target_course AS (
    SELECT c.id
    FROM courses c
    JOIN users u ON u.id = c.user_id
    WHERE u.username = 'bvasquezkeysije'
      AND c.deleted_at IS NULL
      AND lower(c.name) LIKE '%calidad%'
    ORDER BY c.created_at DESC, c.id DESC
    LIMIT 1
),
ordered_sessions AS (
    SELECT
        cs.id,
        ROW_NUMBER() OVER (ORDER BY cs.created_at ASC, cs.id ASC) AS session_order,
        trim(
            regexp_replace(
                COALESCE(cs.name, ''),
                '(?i)^\\s*sesion\\s+[0-9]+\\s*:\\s*',
                ''
            )
        ) AS clean_title
    FROM course_sessions cs
    JOIN target_course tc ON tc.id = cs.course_id
    WHERE cs.deleted_at IS NULL
)
UPDATE course_sessions cs
SET name = CONCAT(
        'SESION ',
        ordered_sessions.session_order,
        ': ',
        CASE
            WHEN ordered_sessions.clean_title = '' THEN 'Sin titulo'
            ELSE ordered_sessions.clean_title
        END
    )
FROM ordered_sessions
WHERE cs.id = ordered_sessions.id;
