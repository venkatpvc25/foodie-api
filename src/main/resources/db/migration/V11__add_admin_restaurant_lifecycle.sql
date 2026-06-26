ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS approved boolean NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS suspended boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_restaurants_admin_lifecycle ON restaurants (approved, suspended, is_open);
