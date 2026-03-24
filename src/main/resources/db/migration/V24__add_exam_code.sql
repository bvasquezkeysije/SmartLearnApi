ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS code VARCHAR(64);

UPDATE exams
SET code = 'EXM-' || LPAD(id::TEXT, 6, '0')
WHERE code IS NULL
   OR BTRIM(code) = '';

CREATE UNIQUE INDEX IF NOT EXISTS uq_exams_code
    ON exams (code);
