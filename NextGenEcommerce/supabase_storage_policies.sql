-- ============================================
-- Storage Policies for product-images bucket
-- Run this in Supabase SQL Editor
-- ============================================

-- Allow anyone to READ product images (public access)
CREATE POLICY "Public read access for product-images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'product-images');

-- Allow anyone to UPLOAD product images (for testing/admin scripts)
-- In production, restrict this to authenticated users
CREATE POLICY "Allow anonymous uploads to product-images"
ON storage.objects FOR INSERT
TO public
WITH CHECK (bucket_id = 'product-images');

-- Allow anyone to UPDATE product images
CREATE POLICY "Allow anonymous updates to product-images"
ON storage.objects FOR UPDATE
TO public
USING (bucket_id = 'product-images')
WITH CHECK (bucket_id = 'product-images');

-- Allow anyone to DELETE product images
CREATE POLICY "Allow anonymous deletes from product-images"
ON storage.objects FOR DELETE
TO public
USING (bucket_id = 'product-images');
