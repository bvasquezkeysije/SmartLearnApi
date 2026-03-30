ALTER TABLE support_conversations
    ADD COLUMN IF NOT EXISTS ticket_type VARCHAR(20);

ALTER TABLE support_conversations
    ADD COLUMN IF NOT EXISTS module_key VARCHAR(60);

UPDATE support_conversations
SET ticket_type = 'support'
WHERE ticket_type IS NULL OR btrim(ticket_type) = '';

ALTER TABLE support_conversations
    ALTER COLUMN ticket_type SET DEFAULT 'support';

ALTER TABLE support_conversations
    ALTER COLUMN ticket_type SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_support_conversations_ticket_type_status
    ON support_conversations (ticket_type, status, updated_at DESC);
