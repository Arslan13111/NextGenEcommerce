-- ============================================
-- Storage Policies for return-images bucket
-- Run this in Supabase SQL Editor
-- ============================================

-- Ensure the bucket exists and is public
INSERT INTO storage.buckets (id, name, public) 
VALUES ('return-images', 'return-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Drop existing policies if they exist (to avoid errors on re-run)
DROP POLICY IF EXISTS "Public read access for return-images" ON storage.objects;
DROP POLICY IF EXISTS "Allow authenticated uploads to return-images" ON storage.objects;
DROP POLICY IF EXISTS "Allow users to update own return images" ON storage.objects;
DROP POLICY IF EXISTS "Allow users to delete own return images" ON storage.objects;

-- Allow anyone to READ return images (needed so admin panel can display them)
CREATE POLICY "Public read access for return-images"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'return-images');

-- Allow ANY authenticated user to UPLOAD return images
CREATE POLICY "Allow authenticated uploads to return-images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'return-images');

-- Allow authenticated users to UPDATE their own images
CREATE POLICY "Allow users to update own return images"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'return-images' AND auth.uid()::text = (storage.foldername(name))[2])
WITH CHECK (bucket_id = 'return-images');

-- Allow authenticated users to DELETE their own images
CREATE POLICY "Allow users to delete own return images"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'return-images' AND auth.uid()::text = (storage.foldername(name))[2]);
