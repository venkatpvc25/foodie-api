ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS type varchar(100),
    ADD COLUMN IF NOT EXISTS target_type varchar(100),
    ADD COLUMN IF NOT EXISTS target_id uuid,
    ADD COLUMN IF NOT EXISTS route varchar(255);

CREATE INDEX IF NOT EXISTS idx_notifications_target ON notifications (target_type, target_id);
