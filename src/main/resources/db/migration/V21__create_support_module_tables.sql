CREATE TABLE IF NOT EXISTS support_conversations (
    id BIGSERIAL PRIMARY KEY,
    requester_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    assigned_admin_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'open',
    priority VARCHAR(20) NOT NULL DEFAULT 'normal',
    channel_preference VARCHAR(20) NOT NULL DEFAULT 'chat',
    whatsapp_number VARCHAR(40) NULL,
    call_number VARCHAR(40) NULL,
    last_message_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_conversations_requester
    ON support_conversations (requester_user_id, deleted_at, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_support_conversations_assigned
    ON support_conversations (assigned_admin_user_id, deleted_at, updated_at DESC);

CREATE TABLE IF NOT EXISTS support_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL REFERENCES support_conversations(id) ON DELETE CASCADE,
    sender_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    sender_role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    read_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_messages_conversation
    ON support_messages (conversation_id, deleted_at, created_at ASC);

CREATE TABLE IF NOT EXISTS support_call_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    handled_by_admin_user_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    phone_number VARCHAR(40) NOT NULL,
    preferred_schedule VARCHAR(255) NULL,
    reason VARCHAR(600) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    handled_at TIMESTAMP NULL,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_support_call_requests_requester
    ON support_call_requests (requester_user_id, deleted_at, created_at DESC);

