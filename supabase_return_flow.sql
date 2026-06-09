-- ============================================================
-- Full Return Flow: New Status Values
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- Step 1: Drop the existing status constraint (drop both possible names)
ALTER TABLE orders DROP CONSTRAINT IF EXISTS valid_status;
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;

-- Step 2: Re-create with all status values including new return stages
ALTER TABLE orders
ADD CONSTRAINT orders_status_check CHECK (
    status IN (
        'PENDING',
        'CONFIRMED',
        'PROCESSING',
        'PACKED',
        'READY_FOR_PICKUP',
        'SHIPPED',
        'OUT_FOR_DELIVERY',
        'DELIVERED',
        'CANCELLED',
        'RETURN_REQUESTED',
        'RETURN_APPROVED',
        'RETURN_IN_TRANSIT',
        'RETURN_RECEIVED',
        'RETURNED',
        'RETURN_REJECTED'
    )
);

-- Step 3: Ensure retailers table has total_revenue column for deduction
ALTER TABLE retailers
ADD COLUMN IF NOT EXISTS total_revenue DECIMAL(12,2) NOT NULL DEFAULT 0.00;

-- Step 4: RLS — Allow retailers to read return_images on their own orders
-- (if you have a strict RLS policy on orders, make sure retailers can SELECT rows
--  where retailer_id matches their record)
-- The existing "Retailers can view their own orders" policy should already cover this.
-- If not, add:
-- CREATE POLICY "Retailers can view their orders"
--   ON orders FOR SELECT
--   USING (
--     retailer_id IN (SELECT id FROM retailers WHERE user_id = auth.uid())
--   );

-- Step 5: RLS — Allow delivery partners to update orders to RETURN_IN_TRANSIT and RETURN_RECEIVED
-- Extend your existing delivery partner UPDATE policy or add:
-- CREATE POLICY "Delivery partners can update return orders"
--   ON orders FOR UPDATE
--   USING (
--     status IN ('RETURN_APPROVED', 'RETURN_IN_TRANSIT')
--   )
--   WITH CHECK (
--     status IN ('RETURN_IN_TRANSIT', 'RETURN_RECEIVED')
--   );

-- Step 6: RLS — Allow retailers to update orders to RETURN_APPROVED, RETURN_REJECTED, RETURNED
-- Extend your existing retailer UPDATE policy or add:
-- CREATE POLICY "Retailers can approve or complete returns"
--   ON orders FOR UPDATE
--   USING (
--     retailer_id IN (SELECT id FROM retailers WHERE user_id = auth.uid())
--     AND status IN ('RETURN_REQUESTED', 'RETURN_RECEIVED')
--   )
--   WITH CHECK (
--     status IN ('RETURN_APPROVED', 'RETURN_REJECTED', 'RETURNED')
--   );
