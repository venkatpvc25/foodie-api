CREATE TABLE IF NOT EXISTS notification_device_tokens (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token text NOT NULL UNIQUE,
    platform varchar(50),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notification_device_tokens_user ON notification_device_tokens (user_id);
