-- =====================================================
-- SUPABASE ROW LEVEL SECURITY (RLS) POLICIES
-- Admin-Only Product Management
-- =====================================================
-- Run this SQL in your Supabase Dashboard: SQL Editor
-- =====================================================

-- IMPORTANT: Replace 'f0d36218-f72c-468e-a0c6-6e46f28da4d7' with your actual admin user ID
-- You can find this in Supabase Dashboard > Authentication > Users > Copy UID

-- =====================================================
-- STEP 1: Create a helper function to check if user is admin
-- =====================================================

CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN AS $$
BEGIN
  -- Check if user has admin role in users table
  -- OR if user ID matches the hardcoded admin ID (fallback)
  RETURN EXISTS (
    SELECT 1 FROM public.users
    WHERE id = auth.uid()
    AND role = 'admin'
  )
  OR auth.uid()::text = 'f0d36218-f72c-468e-a0c6-6e46f28da4d7';  -- Your admin UID
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- =====================================================
-- STEP 2: Enable RLS on products table
-- =====================================================

ALTER TABLE public.products ENABLE ROW LEVEL SECURITY;

-- =====================================================
-- STEP 3: Drop existing policies (if any) to avoid conflicts
-- =====================================================

DROP POLICY IF EXISTS "Allow public read access to products" ON public.products;
DROP POLICY IF EXISTS "Allow admin insert products" ON public.products;
DROP POLICY IF EXISTS "Allow admin update products" ON public.products;
DROP POLICY IF EXISTS "Allow admin delete products" ON public.products;

-- =====================================================
-- STEP 4: Create RLS policies for products table
-- =====================================================

-- Policy 1: Anyone can read products (SELECT)
CREATE POLICY "Allow public read access to products"
ON public.products
FOR SELECT
USING (true);

-- Policy 2: Only admins can insert products (INSERT)
CREATE POLICY "Allow admin insert products"
ON public.products
FOR INSERT
WITH CHECK (public.is_admin());

-- Policy 3: Only admins can update products (UPDATE)
CREATE POLICY "Allow admin update products"
ON public.products
FOR UPDATE
USING (public.is_admin())
WITH CHECK (public.is_admin());

-- Policy 4: Only admins can delete products (DELETE)
CREATE POLICY "Allow admin delete products"
ON public.products
FOR DELETE
USING (public.is_admin());

-- =====================================================
-- STEP 5: Set up RLS for product-images storage bucket
-- =====================================================

-- First, ensure the bucket exists and has the right settings
-- Run these in Storage > Policies section or via SQL:

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Allow public read access to product images" ON storage.objects;
DROP POLICY IF EXISTS "Allow admin upload product images" ON storage.objects;
DROP POLICY IF EXISTS "Allow admin update product images" ON storage.objects;
DROP POLICY IF EXISTS "Allow admin delete product images" ON storage.objects;

-- Policy: Anyone can view product images
CREATE POLICY "Allow public read access to product images"
ON storage.objects
FOR SELECT
USING (bucket_id = 'product-images');

-- Policy: Only admins can upload product images
CREATE POLICY "Allow admin upload product images"
ON storage.objects
FOR INSERT
WITH CHECK (
  bucket_id = 'product-images'
  AND public.is_admin()
);

-- Policy: Only admins can update product images
CREATE POLICY "Allow admin update product images"
ON storage.objects
FOR UPDATE
USING (
  bucket_id = 'product-images'
  AND public.is_admin()
)
WITH CHECK (
  bucket_id = 'product-images'
  AND public.is_admin()
);

-- Policy: Only admins can delete product images
CREATE POLICY "Allow admin delete product images"
ON storage.objects
FOR DELETE
USING (
  bucket_id = 'product-images'
  AND public.is_admin()
);

-- =====================================================
-- STEP 6: Ensure users table has role column
-- =====================================================

-- Add role column if it doesn't exist
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public'
    AND table_name = 'users'
    AND column_name = 'role'
  ) THEN
    ALTER TABLE public.users ADD COLUMN role TEXT DEFAULT 'customer';
  END IF;
END $$;

-- =====================================================
-- STEP 7: Set a user as admin (run this for your admin user)
-- =====================================================

-- Replace 'f0d36218-f72c-468e-a0c6-6e46f28da4d7' with your actual admin user ID
-- UPDATE public.users SET role = 'admin' WHERE id = 'f0d36218-f72c-468e-a0c6-6e46f28da4d7';

-- Or set admin by email:
-- UPDATE public.users SET role = 'admin' WHERE email = 'your-admin-email@example.com';

-- =====================================================
-- VERIFICATION QUERIES (Optional - to test your setup)
-- =====================================================

-- Check if RLS is enabled:
-- SELECT tablename, rowsecurity FROM pg_tables WHERE schemaname = 'public' AND tablename = 'products';

-- Check existing policies:
-- SELECT * FROM pg_policies WHERE tablename = 'products';

-- Check if is_admin function exists:
-- SELECT proname FROM pg_proc WHERE proname = 'is_admin';

-- Test is_admin function (replace with actual user ID):
-- SET request.jwt.claim.sub = 'f0d36218-f72c-468e-a0c6-6e46f28da4d7';
-- SELECT public.is_admin();
