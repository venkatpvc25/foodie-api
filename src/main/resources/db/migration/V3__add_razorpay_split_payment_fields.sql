ALTER TABLE restaurants
    ADD COLUMN IF NOT EXISTS razorpay_linked_account_id varchar(255);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS restaurant_payout_amount numeric(12, 2),
    ADD COLUMN IF NOT EXISTS platform_commission_amount numeric(12, 2),
    ADD COLUMN IF NOT EXISTS restaurant_razorpay_transfer_id varchar(255),
    ADD COLUMN IF NOT EXISTS admin_razorpay_transfer_id varchar(255);

CREATE INDEX IF NOT EXISTS idx_restaurants_razorpay_linked_account_id
    ON restaurants (razorpay_linked_account_id);
