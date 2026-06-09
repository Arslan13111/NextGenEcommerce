-- ============================================================
-- Shipping Address Access for Retailers & Delivery Partners
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- Allow any authenticated user to read shipping addresses.
-- Retailers need them to know where to send orders.
-- Delivery partners need them to know where to deliver.
-- PostgREST joins (shipping_addresses!shipping_address_id(*)) require
-- the joining user to have SELECT on shipping_addresses — without this
-- the joined field returns null even when the FK resolves correctly.

DROP POLICY IF EXISTS "Authenticated users can read shipping addresses" ON shipping_addresses;
CREATE POLICY "Authenticated users can read shipping addresses"
ON shipping_addresses FOR SELECT
TO authenticated
USING (true);
