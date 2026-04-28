-- ============================================
-- Supabase Database Setup Script
-- NextGenEcommerce Android App
-- ============================================
-- Run this entire script in Supabase SQL Editor
-- Dashboard → SQL Editor → New Query → Paste and Run
-- ============================================

-- ============================================
-- 1. CREATE USERS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT auth.uid(),
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL,
    phone TEXT DEFAULT '',
    profile_image_url TEXT,
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
    role TEXT NOT NULL DEFAULT 'customer',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Add index on email for faster lookups
CREATE INDEX IF NOT EXISTS users_email_idx ON users(email);

-- Add index on id for faster joins
CREATE INDEX IF NOT EXISTS users_id_idx ON users(id);

COMMENT ON TABLE users IS 'User profile data for NextGenEcommerce app';
COMMENT ON COLUMN users.id IS 'User ID from Supabase Auth (auth.uid())';
COMMENT ON COLUMN users.email IS 'User email address';
COMMENT ON COLUMN users.name IS 'User full name';
COMMENT ON COLUMN users.phone IS 'User phone number';
COMMENT ON COLUMN users.profile_image_url IS 'URL to profile picture in Supabase Storage';
COMMENT ON COLUMN users.created_at IS 'Unix timestamp of account creation';
COMMENT ON COLUMN users.role IS 'User role: customer or admin';

-- ============================================
-- 2. ENABLE ROW LEVEL SECURITY
-- ============================================

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- ============================================
-- 3. CREATE RLS POLICIES FOR USERS TABLE
-- ============================================

-- Drop existing policies if they exist (for re-running script)
DROP POLICY IF EXISTS "Users can view their own profile" ON users;
DROP POLICY IF EXISTS "Users can insert their own profile" ON users;
DROP POLICY IF EXISTS "Users can update their own profile" ON users;
DROP POLICY IF EXISTS "Users can delete their own profile" ON users;

-- Policy: Users can view their own profile
CREATE POLICY "Users can view their own profile"
ON users FOR SELECT
TO authenticated
USING (auth.uid() = id);

-- Policy: Users can insert their own profile (during registration)
CREATE POLICY "Users can insert their own profile"
ON users FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = id);

-- Policy: Users can update their own profile
CREATE POLICY "Users can update their own profile"
ON users FOR UPDATE
TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

-- Policy: Users can delete their own profile (optional)
CREATE POLICY "Users can delete their own profile"
ON users FOR DELETE
TO authenticated
USING (auth.uid() = id);

-- ============================================
-- 4. CREATE STORAGE BUCKETS
-- ============================================
-- Note: Storage buckets must be created via Dashboard or API
-- This is just documentation of what's needed

-- MANUAL STEP REQUIRED:
-- 1. Go to Storage in Supabase Dashboard
-- 2. Create bucket named "profiles" (make it PUBLIC)
-- 3. Apply the storage policies below

-- ============================================
-- 5. STORAGE POLICIES FOR PROFILES BUCKET
-- ============================================

-- Drop existing storage policies if they exist
DROP POLICY IF EXISTS "Public profiles read access" ON storage.objects;
DROP POLICY IF EXISTS "Users can upload their profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can update their profile image" ON storage.objects;
DROP POLICY IF EXISTS "Users can delete their profile image" ON storage.objects;

-- Policy: Allow public read access to profile images
CREATE POLICY "Public profiles read access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'profiles');

-- Policy: Allow authenticated users to upload their own profile images
CREATE POLICY "Users can upload their profile image"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- Policy: Allow users to update their own profile images
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

-- Policy: Allow users to delete their own profile images
CREATE POLICY "Users can delete their profile image"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

-- ============================================
-- 6. CREATE TRIGGER FOR UPDATED_AT
-- ============================================

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger on users table
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- 7. CREATE FUNCTION TO HANDLE NEW USER SIGNUP
-- ============================================
-- This function automatically creates a user profile
-- when a new user signs up via Supabase Auth

CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.users (id, email, name, created_at)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'name', 'User'),
        EXTRACT(EPOCH FROM NOW())::BIGINT
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create trigger to automatically create user profile on signup
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- ============================================
-- 8. VERIFY SETUP
-- ============================================

-- Check if users table exists
SELECT
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'users';

-- Check users table columns
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'users'
ORDER BY ordinal_position;

-- Check RLS is enabled
SELECT
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables
WHERE tablename = 'users';

-- Check policies
SELECT
    policyname,
    tablename,
    cmd,
    roles
FROM pg_policies
WHERE tablename = 'users';

-- ============================================
-- SETUP COMPLETE!
-- ============================================

-- Next steps:
-- 1. Go to Storage in Dashboard and create "profiles" bucket (PUBLIC)
-- 2. Go to Authentication → Providers and enable Email provider
-- 3. (Optional) Configure Google OAuth for social login
-- 4. Update your app's SupabaseConfig.kt with project URL and anon key
-- 5. Test registration and login in your Android app

-- ============================================
-- NOTES
-- ============================================

-- Profile Image URLs will be in format:
-- https://YOUR_PROJECT.supabase.co/storage/v1/object/public/profiles/{user_id}/{filename}

-- To manually insert a test user (for testing only):
-- INSERT INTO users (id, email, name, phone, role)
-- VALUES (
--     'test-uuid-here',
--     'test@example.com',
--     'Test User',
--     '+1234567890',
--     'customer'
-- );

-- To view all users:
-- SELECT * FROM users;

-- To delete all users (CAREFUL!):
-- DELETE FROM users;

-- ============================================
-- TROUBLESHOOTING
-- ============================================

-- If you get "permission denied" errors:
-- 1. Make sure RLS is enabled: ALTER TABLE users ENABLE ROW LEVEL SECURITY;
-- 2. Make sure policies exist: SELECT * FROM pg_policies WHERE tablename = 'users';
-- 3. Make sure you're authenticated when testing

-- If profile images won't upload:
-- 1. Make sure "profiles" bucket exists in Storage
-- 2. Make sure bucket is PUBLIC
-- 3. Make sure storage policies are created (see section 5)
-- 4. Check that the user is authenticated

-- If users table won't insert:
-- 1. Check that auth.uid() matches the id you're inserting
-- 2. Make sure user is authenticated
-- 3. Check INSERT policy allows the operation

-- ============================================
