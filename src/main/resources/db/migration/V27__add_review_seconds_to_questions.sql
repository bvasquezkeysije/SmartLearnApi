ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS review_seconds INTEGER NOT NULL DEFAULT 10;

UPDATE questions
SET review_seconds = 10
WHERE review_seconds IS NULL OR review_seconds <= 0;
