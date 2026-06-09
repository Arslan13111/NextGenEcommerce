-- ═══════════════════════════════════════════════════════════════════════════
-- Safepay Payment Integration — Supabase Migration
-- Run this in your Supabase SQL Editor (Dashboard > SQL Editor > New Query)
-- ═══════════════════════════════════════════════════════════════════════════

-- 1. Add Safepay tracker columns to payment_transactions table
ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS safepay_tracker    TEXT,
    ADD COLUMN IF NOT EXISTS safepay_token      TEXT,
    ADD COLUMN IF NOT EXISTS gateway            TEXT DEFAULT 'CASH_ON_DELIVERY';

-- 2. Add safepay_tracker to orders table (useful for quick lookup)
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS safepay_tracker TEXT;

-- 3. Update payment_method column in orders if it doesn't already support SAFEPAY
-- (No change needed if you're storing as TEXT — just verifying the enum/check)
-- If you have a CHECK constraint on payment_method, run:
-- ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_payment_method_check;
-- ALTER TABLE orders ADD CONSTRAINT orders_payment_method_check
--   CHECK (payment_method IN ('CASH_ON_DELIVERY', 'SAFEPAY', 'EASYPAISA', 'JAZZCASH'));

-- 4. Update existing PENDING payment_transactions to mark their gateway
UPDATE payment_transactions
SET gateway = 'CASH_ON_DELIVERY'
WHERE gateway IS NULL OR gateway = '';

-- 5. Create index for faster payment lookup
CREATE INDEX IF NOT EXISTS idx_payment_transactions_safepay_tracker
    ON payment_transactions(safepay_tracker)
    WHERE safepay_tracker IS NOT NULL;

-- 6. Create a helper view for admin dashboard payment summary
CREATE OR REPLACE VIEW payment_summary AS
SELECT
    o.id            AS order_id,
    o.order_number,
    o.user_id,
    o.total,
    o.payment_method,
    o.payment_status,
    pt.safepay_tracker,
    pt.gateway,
    pt.status       AS transaction_status,
    pt.created_at   AS payment_created_at,
    pt.completed_at AS payment_completed_at
FROM orders o
LEFT JOIN payment_transactions pt ON pt.order_id = o.id;

-- Done! ✅
-- Your payment_transactions table now supports Safepay tracker fields.
