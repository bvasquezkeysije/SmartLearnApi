ALTER TABLE exam_group_session_members
    ADD COLUMN IF NOT EXISTS room_session_token VARCHAR(128);

ALTER TABLE exam_group_session_members
    ADD COLUMN IF NOT EXISTS room_session_issued_at TIMESTAMP;

ALTER TABLE exam_group_session_members
    ADD COLUMN IF NOT EXISTS room_session_expires_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_exam_group_session_members_room_token
    ON exam_group_session_members (room_session_token)
    WHERE deleted_at IS NULL AND room_session_token IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_exam_group_session_members_session_room_token
    ON exam_group_session_members (session_id, room_session_token)
    WHERE deleted_at IS NULL AND room_session_token IS NOT NULL;
