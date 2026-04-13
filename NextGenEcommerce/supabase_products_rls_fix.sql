-- Fix RLS policy for products table to allow anonymous inserts
-- Run this in Supabase SQL Editor

-- Drop the existing authenticated-only policy
DROP POLICY IF EXISTS "Authenticated users can insert products" ON products;

-- Create policy allowing public inserts (for testing/admin scripts)
CREATE POLICY "Allow public insert to products"
ON products FOR INSERT
TO public
WITH CHECK (true);
