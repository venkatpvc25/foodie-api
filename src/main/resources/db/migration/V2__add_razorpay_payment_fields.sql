ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS razorpay_order_id varchar(255),
    ADD COLUMN IF NOT EXISTS razorpay_payment_id varchar(255),
    ADD COLUMN IF NOT EXISTS razorpay_signature varchar(255);

CREATE INDEX IF NOT EXISTS idx_orders_razorpay_order_id ON orders (razorpay_order_id);
