ALTER TABLE users
    ADD COLUMN IF NOT EXISTS blocked boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_users_blocked ON users (blocked);
