CREATE TABLE IF NOT EXISTS restaurant_ratings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    restaurant_id uuid NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    rating int NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT unique_restaurant_rating_per_order UNIQUE (order_id, restaurant_id)
);

CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_restaurant ON restaurant_ratings (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_ratings_customer ON restaurant_ratings (customer_id);

CREATE TABLE IF NOT EXISTS menu_item_ratings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    restaurant_id uuid NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    menu_item_id uuid NOT NULL REFERENCES menu_items (id) ON DELETE CASCADE,
    rating int NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT unique_menu_item_rating_per_order UNIQUE (order_id, menu_item_id)
);

CREATE INDEX IF NOT EXISTS idx_menu_item_ratings_menu_item ON menu_item_ratings (menu_item_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_ratings_customer ON menu_item_ratings (customer_id);

CREATE TABLE IF NOT EXISTS delivery_partner_ratings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    delivery_partner_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating int NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT unique_delivery_partner_rating_per_order UNIQUE (order_id, delivery_partner_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_partner_ratings_partner ON delivery_partner_ratings (delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_delivery_partner_ratings_customer ON delivery_partner_ratings (customer_id);

CREATE TABLE IF NOT EXISTS delivery_partner_tips (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    customer_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    delivery_partner_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    amount numeric(12, 2) NOT NULL CHECK (amount > 0),
    note text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now(),
    CONSTRAINT unique_delivery_partner_tip_per_order UNIQUE (order_id, delivery_partner_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_partner_tips_partner ON delivery_partner_tips (delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_delivery_partner_tips_customer ON delivery_partner_tips (customer_id);
