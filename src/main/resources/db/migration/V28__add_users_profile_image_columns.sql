ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_image_data TEXT,
    ADD COLUMN IF NOT EXISTS profile_image_scale DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS profile_image_offset_x DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS profile_image_offset_y DOUBLE PRECISION;
