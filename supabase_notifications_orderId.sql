-- ============================================================
-- Add order_id column to push_notifications
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

ALTER TABLE push_notifications
    ADD COLUMN IF NOT EXISTS order_id UUID;
