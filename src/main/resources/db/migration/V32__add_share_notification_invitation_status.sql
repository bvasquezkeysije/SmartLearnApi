ALTER TABLE share_notifications
    ADD COLUMN IF NOT EXISTS invitation_status VARCHAR(20) NOT NULL DEFAULT 'accepted';

ALTER TABLE share_notifications
    ADD COLUMN IF NOT EXISTS invitation_responded_at TIMESTAMP NULL;

ALTER TABLE share_notifications
    ADD COLUMN IF NOT EXISTS exam_role VARCHAR(20) NULL;

ALTER TABLE share_notifications
    ADD COLUMN IF NOT EXISTS exam_can_share BOOLEAN NULL;

UPDATE share_notifications
SET invitation_status = 'accepted'
WHERE invitation_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_share_notifications_recipient_invitation_status
    ON share_notifications (recipient_user_id, invitation_status, deleted_at, created_at DESC);
