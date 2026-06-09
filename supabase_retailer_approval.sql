-- ============================================================
-- Retailer Approval System + Targeted Notifications
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- 0. Add recipient_id to push_notifications for user-specific targeting
--    (order status updates go to the specific customer, not all customers)
ALTER TABLE push_notifications
ADD COLUMN IF NOT EXISTS recipient_id UUID REFERENCES auth.users(id) ON DELETE SET NULL;

-- Allow customers to see notifications targeted at them
DROP POLICY IF EXISTS "Users can read their notifications" ON push_notifications;
CREATE POLICY "Users can read their notifications"
ON push_notifications FOR SELECT
TO authenticated
USING (
    recipient_role = 'all'
    OR recipient_role = (
        SELECT role FROM users WHERE id = auth.uid() LIMIT 1
    )
    AND (recipient_id IS NULL OR recipient_id = auth.uid())
);

-- 1. Allow admins to update any retailer profile (including is_verified)
DROP POLICY IF EXISTS "Admins can update retailer approval" ON retailers;
CREATE POLICY "Admins can update retailer approval"
ON retailers FOR UPDATE
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
);

-- 2. Allow admins to view all retailers
DROP POLICY IF EXISTS "Admins can view all retailers" ON retailers;
CREATE POLICY "Admins can view all retailers"
ON retailers FOR SELECT
TO authenticated
USING (true);

-- 3. Update product management policy to require is_verified = true for retailers
--    Admins bypass this check entirely.
DROP POLICY IF EXISTS "Retailers can manage their own products" ON products;
CREATE POLICY "Verified retailers can manage their own products"
ON products FOR ALL
TO authenticated
USING (
    -- Verified retailer owns this product
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
          AND retailers.id = products.retailer_id
          AND retailers.is_verified = true
    )
    OR
    -- Admin can manage any product
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
);
