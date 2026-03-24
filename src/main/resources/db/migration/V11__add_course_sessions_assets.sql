ALTER TABLE course_sessions
    ADD COLUMN IF NOT EXISTS video_link VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS cover_image_data TEXT,
    ADD COLUMN IF NOT EXISTS pdf_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pdf_file_data TEXT,
    ADD COLUMN IF NOT EXISTS word_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS word_file_data TEXT;
