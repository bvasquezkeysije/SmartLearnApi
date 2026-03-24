UPDATE course_sessions cs
SET name = CONCAT(
        'SESION ',
        substring(cs.name from '(?i)^\\s*sesion\\s+([0-9]+)\\s*:'),
        ': ',
        CASE
            WHEN trim(COALESCE(substring(cs.name from '(?i)^\\s*sesion\\s+[0-9]+\\s*:\\s*(.*)$'), '')) = ''
                THEN 'Sin titulo'
            ELSE trim(substring(cs.name from '(?i)^\\s*sesion\\s+[0-9]+\\s*:\\s*(.*)$'))
        END
    )
FROM courses c
JOIN users u ON u.id = c.user_id
WHERE cs.course_id = c.id
  AND u.username = 'bvasquezkeysije'
  AND lower(c.name) LIKE '%calidad%'
  AND cs.name ~* '^\\s*sesion\\s+[0-9]+\\s*:';
