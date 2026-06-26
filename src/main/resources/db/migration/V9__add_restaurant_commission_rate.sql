ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS commission_rate numeric(5, 4);
