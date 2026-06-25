CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email varchar(255) NOT NULL UNIQUE,
    name varchar(150) NOT NULL,
    phone varchar(20) UNIQUE,
    password varchar(255) NOT NULL,
    role varchar(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS addresses (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title varchar(80),
    address_line1 varchar(255) NOT NULL,
    address_line2 varchar(255),
    city varchar(100),
    state varchar(100),
    latitude numeric(10, 7),
    longitude numeric(10, 7),
    is_default boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS restaurants (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(255) NOT NULL,
    description text,
    image_url varchar(255),
    phone varchar(20),
    address text,
    latitude numeric(10, 7),
    longitude numeric(10, 7),
    is_open boolean NOT NULL DEFAULT true,
    created_by uuid NOT NULL REFERENCES users (id),
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_restaurants_name ON restaurants (name);
CREATE INDEX IF NOT EXISTS idx_restaurants_created_by ON restaurants (created_by);

CREATE TABLE IF NOT EXISTS categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    display_order int NOT NULL DEFAULT 0,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS menu_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id uuid NOT NULL REFERENCES restaurants (id) ON DELETE CASCADE,
    category_id uuid REFERENCES categories (id) ON DELETE SET NULL,
    name varchar(255) NOT NULL,
    description text,
    price numeric(12, 2) NOT NULL,
    image_url varchar(255),
    veg boolean NOT NULL DEFAULT false,
    is_available boolean NOT NULL DEFAULT true,
    display_order int NOT NULL DEFAULT 0,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS carts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    restaurant_id uuid REFERENCES restaurants (id),
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS cart_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id uuid NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    menu_item_id uuid NOT NULL REFERENCES menu_items (id),
    quantity int NOT NULL,
    price numeric(12, 2) NOT NULL,
    created_at timestamp with time zone DEFAULT now(),
    CONSTRAINT unique_cart_menu_item UNIQUE (cart_id, menu_item_id)
);

CREATE TABLE IF NOT EXISTS orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number varchar(255) NOT NULL UNIQUE,
    user_id uuid NOT NULL REFERENCES users (id),
    restaurant_id uuid NOT NULL REFERENCES restaurants (id),
    delivery_partner_id uuid REFERENCES users (id),
    address_id uuid NOT NULL REFERENCES addresses (id),
    status varchar(40) NOT NULL,
    subtotal numeric(12, 2) NOT NULL,
    delivery_charge numeric(12, 2) NOT NULL,
    tax numeric(12, 2) NOT NULL,
    total numeric(12, 2) NOT NULL,
    payment_method varchar(40) NOT NULL,
    payment_status varchar(40) NOT NULL,
    notes text,
    created_at timestamp with time zone DEFAULT now(),
    updated_at timestamp with time zone DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders (user_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant ON orders (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_partner ON orders (delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);

CREATE TABLE IF NOT EXISTS order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    menu_item_id uuid NOT NULL,
    name varchar(255) NOT NULL,
    price numeric(12, 2) NOT NULL,
    quantity int NOT NULL,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS notifications (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title varchar(255) NOT NULL,
    message text NOT NULL,
    is_read boolean NOT NULL DEFAULT false,
    created_at timestamp with time zone DEFAULT now()
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    token varchar(255) NOT NULL UNIQUE,
    user_id uuid REFERENCES users (id) ON DELETE CASCADE,
    expiry_date timestamp with time zone NOT NULL,
    revoked boolean NOT NULL DEFAULT false
);
