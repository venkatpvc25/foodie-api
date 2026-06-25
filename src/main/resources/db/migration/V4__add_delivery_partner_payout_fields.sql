ALTER TABLE users
    ADD COLUMN IF NOT EXISTS razorpay_linked_account_id varchar(255);

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_partner_payout_amount numeric(12, 2),
    ADD COLUMN IF NOT EXISTS delivery_partner_razorpay_transfer_id varchar(255);

CREATE INDEX IF NOT EXISTS idx_users_razorpay_linked_account_id
    ON users (razorpay_linked_account_id);
