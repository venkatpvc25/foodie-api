CREATE TABLE IF NOT EXISTS coupons (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code varchar(80) NOT NULL UNIQUE,
    description text,
    discount_type varchar(40) NOT NULL,
    discount_value numeric(12, 2) NOT NULL,
    max_discount_amount numeric(12, 2),
    min_order_amount numeric(12, 2) NOT NULL DEFAULT 0,
    restaurant_id uuid REFERENCES restaurants (id) ON DELETE CASCADE,
    active boolean NOT NULL DEFAULT true,
    valid_from timestamp with time zone,
    valid_to timestamp with time zone,
    usage_limit int,
    used_count int NOT NULL DEFAULT 0,
    created_by uuid REFERENCES users (id),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT chk_coupons_discount_value_positive CHECK (discount_value > 0),
    CONSTRAINT chk_coupons_min_order_amount_non_negative CHECK (min_order_amount >= 0),
    CONSTRAINT chk_coupons_max_discount_amount_positive CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT chk_coupons_usage_limit_positive CHECK (usage_limit IS NULL OR usage_limit > 0)
);

CREATE INDEX IF NOT EXISTS idx_coupons_code ON coupons (code);
CREATE INDEX IF NOT EXISTS idx_coupons_restaurant ON coupons (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_coupons_active ON coupons (active);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS coupon_id uuid REFERENCES coupons (id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS coupon_code varchar(80),
    ADD COLUMN IF NOT EXISTS discount_amount numeric(12, 2) NOT NULL DEFAULT 0;
