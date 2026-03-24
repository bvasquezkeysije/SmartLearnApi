CREATE TABLE IF NOT EXISTS share_notifications (
    id BIGSERIAL PRIMARY KEY,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    share_link_id BIGINT NOT NULL REFERENCES share_links(id) ON DELETE CASCADE,
    resource_type VARCHAR(30) NOT NULL,
    resource_id BIGINT NOT NULL,
    resource_name VARCHAR(255) NULL,
    message VARCHAR(600) NULL,
    read_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_share_notifications_recipient_deleted
    ON share_notifications (recipient_user_id, deleted_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_share_notifications_sender_deleted
    ON share_notifications (sender_user_id, deleted_at, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_share_notifications_share_link
    ON share_notifications (share_link_id);

