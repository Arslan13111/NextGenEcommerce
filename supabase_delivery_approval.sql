-- ============================================================
-- Delivery Partner Approval System
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- 1. Add is_verified column to delivery_partners
--    New delivery partners default to false (pending approval)
ALTER TABLE delivery_partners
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT false;

-- 2. RLS: Allow admin to view all delivery partners
DROP POLICY IF EXISTS "Admins can view all delivery partners" ON delivery_partners;
CREATE POLICY "Admins can view all delivery partners"
ON delivery_partners FOR SELECT
TO authenticated
USING (true);

-- 3. RLS: Allow admin to approve/revoke delivery partners
--    (UPDATE is_verified column)
DROP POLICY IF EXISTS "Admins can update delivery partner approval" ON delivery_partners;
CREATE POLICY "Admins can update delivery partner approval"
ON delivery_partners FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
);

-- 4. RLS: Allow delivery partners to update their own orders
--    (needed for reassignment — admin or delivery partner with matching delivery_partner_id)
DROP POLICY IF EXISTS "Admin can reassign delivery partner on orders" ON orders;
CREATE POLICY "Admin can reassign delivery partner on orders"
ON orders FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
)
WITH CHECK (
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
);
