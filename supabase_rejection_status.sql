-- ============================================================
-- Rejection Status for Retailers & Delivery Partners
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- Add is_rejected column to retailers
ALTER TABLE retailers
ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN NOT NULL DEFAULT false;

-- Add is_rejected column to delivery_partners
ALTER TABLE delivery_partners
ADD COLUMN IF NOT EXISTS is_rejected BOOLEAN NOT NULL DEFAULT false;

-- Ensure RLS allows admins to update is_rejected on retailers
-- (existing "Admins can update retailer approval" policy covers UPDATE,
--  but if you dropped and recreated it, re-run from supabase_retailer_approval.sql)

-- Ensure RLS allows admins to update is_rejected on delivery_partners
-- (existing "Admins can update delivery partner approval" policy covers UPDATE,
--  re-run from supabase_delivery_approval.sql if needed)
