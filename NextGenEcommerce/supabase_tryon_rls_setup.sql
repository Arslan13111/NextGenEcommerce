-- ============================================
-- Supabase RLS Setup for Try-On Architecture
-- NextGenEcommerce - Refactored Image Handling
-- ============================================
-- Run this in Supabase SQL Editor after the main setup script.
-- This ensures correct RLS for the try-on feature.
-- ============================================

-- ============================================
-- 0. HELPER FUNCTION - Admin check (SECURITY DEFINER)
-- ============================================
-- This function bypasses RLS to check admin status,
-- preventing infinite recursion when admin policies
-- query the users table.

CREATE OR REPLACE FUNCTION is_admin()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM users
    WHERE id = auth.uid() AND role = 'admin'
  );
$$;

-- ============================================
-- 1. USERS TABLE - RLS Policies
-- ============================================
-- Users can ONLY read their own row.
-- users.profile_image_url is the authoritative avatar source for try-on.

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Drop existing policies (safe to re-run)
DROP POLICY IF EXISTS "Users can view their own profile" ON users;
DROP POLICY IF EXISTS "Users can insert their own profile" ON users;
DROP POLICY IF EXISTS "Users can update their own profile" ON users;
DROP POLICY IF EXISTS "Admins can view all users" ON users;

-- Users can view ONLY their own profile
CREATE POLICY "Users can view their own profile"
ON users FOR SELECT
TO authenticated
USING (auth.uid() = id);

-- Users can insert their own profile (registration)
CREATE POLICY "Users can insert their own profile"
ON users FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

-- Users can update ONLY their own profile (including profile_image_url)
CREATE POLICY "Users can update their own profile"
ON users FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Admin can view all users (for admin dashboard)
-- Uses is_admin() SECURITY DEFINER function to avoid infinite recursion
CREATE POLICY "Admins can view all users"
ON users FOR SELECT
TO authenticated
USING (is_admin());

-- ============================================
-- 2. PRODUCTS TABLE - RLS Policies
-- ============================================
-- Products are publicly readable (for browsing).
-- products.image_url is the ONLY image source for try-on.
-- Only admins can insert/update/delete products.

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Products are publicly readable" ON products;
DROP POLICY IF EXISTS "Admins can insert products" ON products;
DROP POLICY IF EXISTS "Admins can update products" ON products;
DROP POLICY IF EXISTS "Admins can delete products" ON products;

-- Anyone (including anonymous) can read products
CREATE POLICY "Products are publicly readable"
ON products FOR SELECT
TO public
USING (true);

-- Only admins can insert products
CREATE POLICY "Admins can insert products"
ON products FOR INSERT
TO authenticated
WITH CHECK (is_admin());

-- Only admins can update products
CREATE POLICY "Admins can update products"
ON products FOR UPDATE
TO authenticated
USING (is_admin())
WITH CHECK (is_admin());

-- Only admins can delete products
CREATE POLICY "Admins can delete products"
ON products FOR DELETE
TO authenticated
USING (is_admin());

-- ============================================
-- 3. STORAGE POLICIES - product-images bucket
-- ============================================
-- Product images and try-on temp avatars are stored here.
-- Public read access for all product images.
-- Authenticated users can upload temp avatars for try-on.

-- Drop existing storage policies for product-images
DROP POLICY IF EXISTS "Public read product images" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated upload to product-images" ON storage.objects;
DROP POLICY IF EXISTS "Admin manage product images" ON storage.objects;

-- Public can read all product images (needed for try-on API)
CREATE POLICY "Public read product images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'product-images');

-- Authenticated users can upload temp avatars for try-on
CREATE POLICY "Authenticated upload to product-images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'product-images' AND
    (storage.foldername(name))[1] = 'temp-avatars'
);

-- Admins can manage all product images
CREATE POLICY "Admin manage product images"
ON storage.objects FOR ALL
TO authenticated
USING (
    bucket_id = 'product-images' AND
    is_admin()
);

-- ============================================
-- 4. STORAGE POLICIES - profiles bucket
-- ============================================
-- Profile images are stored here.
-- Public read (needed for try-on to use profile image as avatar).
-- Users can only manage their own folder.

-- (These should already exist from supabase_setup.sql, but just in case)
DROP POLICY IF EXISTS "Public profiles read access" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload their profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can update their profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete their profile image" ON storage.objects;

CREATE POLICY "Public profiles read access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'profiles');

CREATE POLICY "Users can upload their profile image"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can update their profile image"
ON storage.objects FOR UPDATE
TO authenticated
USING (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
)
WITH CHECK (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can delete their profile image"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- ============================================
-- 5. VERIFY SETUP
-- ============================================

-- Check RLS is enabled on both tables
SELECT tablename, rowsecurity
FROM pg_tables
WHERE tablename IN ('users', 'products');

-- Check all policies
SELECT policyname, tablename, cmd
FROM pg_policies
WHERE tablename IN ('users', 'products')
ORDER BY tablename, policyname;

-- ============================================
-- ARCHITECTURE NOTES
-- ============================================
--
-- TRY-ON IMAGE FLOW:
--
-- 1. Product Image (clothing):
--    Source: products.image_url (Supabase Storage public URL)
--    Flow:  products table -> image_url -> sent directly to Try-On API
--    NEVER use local_image_name for try-on
--
-- 2. User Avatar (person photo):
--    Source A: users.profile_image_url (existing profile photo)
--    Source B: Freshly uploaded photo -> converted to base64 -> sent to backend
--    Flow:  auth.uid() -> users.id -> profile_image_url -> Try-On API
--
-- 3. Try-On Result:
--    Returned from RapidAPI -> displayed in app
--    Optionally saved to product-images/try-on-results/
--
-- SECURITY:
-- - Users can only read their own profile
-- - Product images are publicly accessible (required for API)
-- - Profile images are publicly accessible (required for try-on URL pass-through)
-- - Temp avatars are uploaded to product-images/temp-avatars/ by authenticated users
-- - Only admins can manage product data
-- - is_admin() uses SECURITY DEFINER to prevent infinite recursion
--
