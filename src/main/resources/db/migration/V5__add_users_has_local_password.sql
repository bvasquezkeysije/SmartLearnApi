ALTER TABLE users
ADD COLUMN IF NOT EXISTS has_local_password BOOLEAN;

UPDATE users
SET has_local_password = CASE
    WHEN LOWER(COALESCE(auth_provider, '')) = 'google' THEN FALSE
    ELSE TRUE
END
WHERE has_local_password IS NULL;

ALTER TABLE users
ALTER COLUMN has_local_password SET DEFAULT FALSE;
