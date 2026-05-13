-- Migration script to fix missing columns and RLS policies in delivery_partners and orders tables
-- Run this in your Supabase SQL Editor

-- 1. Add missing columns
ALTER TABLE delivery_partners 
ADD COLUMN IF NOT EXISTS company_name TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS contact_person TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS company_address TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS contact_phone TEXT NOT NULL DEFAULT '',
ADD COLUMN IF NOT EXISTS company_logo_url TEXT;

ALTER TABLE orders
ADD COLUMN IF NOT EXISTS shipping_address_snapshot TEXT;

-- 2. Ensure helper function exists
CREATE OR REPLACE FUNCTION is_delivery_partner()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'delivery_partner'
    );
$$;

-- 3. Update RLS policies for orders to allow delivery partners to see available orders
DROP POLICY IF EXISTS "Users can view relevant orders" ON orders;
CREATE POLICY "Users can view relevant orders"
ON orders FOR SELECT
USING (
    auth.uid() = user_id
    OR is_admin()
    OR EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = orders.retailer_id
    )
    OR orders.delivery_partner_id = auth.uid()
    OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP')
);

DROP POLICY IF EXISTS "Authorized users can update orders" ON orders;
CREATE POLICY "Authorized users can update orders"
ON orders FOR UPDATE
USING (
    is_admin()
    OR EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = orders.retailer_id
    )
    OR orders.delivery_partner_id = auth.uid()
    OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP' AND delivery_partner_id IS NULL)
);

-- 4. Update RLS policies for order_items
DROP POLICY IF EXISTS "Users can view own order items" ON order_items;
CREATE POLICY "Users can view own order items"
ON order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = order_items.order_id
        AND (
            orders.user_id = auth.uid()
            OR is_admin()
            OR EXISTS (
                SELECT 1 FROM retailers
                WHERE retailers.user_id = auth.uid()
                AND   retailers.id      = orders.retailer_id
            )
            OR orders.delivery_partner_id = auth.uid()
            OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP')
        )
    )
);

-- Refresh PostgREST schema cache
NOTIFY pgrst, 'reload schema';
