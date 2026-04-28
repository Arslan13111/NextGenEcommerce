-- ============================================================
-- Migration: Add color_images and lens_id to products table
-- NextGenEcommerce Android App
-- ============================================================
-- Run this in Supabase Dashboard → SQL Editor → New Query
-- This is safe to run multiple times (IF NOT EXISTS guards)
-- ============================================================

-- Add color_images column (JSONB) for per-color image mapping
-- Stores {"ColorName": "imageUrl", "#FF5733": "imageUrl", ...}
ALTER TABLE products
ADD COLUMN IF NOT EXISTS color_images JSONB NOT NULL DEFAULT '{}';

-- Add lens_id column for Snap Camera Kit live AR try-on
ALTER TABLE products
ADD COLUMN IF NOT EXISTS lens_id TEXT;

-- Verify the columns were added
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name   = 'products'
  AND column_name IN ('color_images', 'lens_id')
ORDER BY column_name;
