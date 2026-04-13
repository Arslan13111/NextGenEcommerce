-- ============================================
-- Supabase Products Table Setup Script
-- NextGenEcommerce Android App
-- ============================================
-- Run this script in Supabase SQL Editor
-- Dashboard → SQL Editor → New Query → Paste and Run
-- ============================================

-- ============================================
-- 1. CREATE PRODUCTS TABLE
-- ============================================

CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    description TEXT DEFAULT '',
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    original_price DECIMAL(10, 2) DEFAULT 0.00,
    category TEXT NOT NULL DEFAULT 'CLOTHING',
    sub_category TEXT DEFAULT '',
    images TEXT[] DEFAULT ARRAY[]::TEXT[],
    image_url TEXT,
    ar_model_url TEXT,
    local_image_name TEXT DEFAULT '',
    sizes TEXT[] DEFAULT ARRAY['S', 'M', 'L', 'XL']::TEXT[],
    colors TEXT[] DEFAULT ARRAY['Black', 'White', 'Blue', 'Red']::TEXT[],
    rating DECIMAL(2, 1) DEFAULT 0.0,
    review_count INTEGER DEFAULT 0,
    stock INTEGER DEFAULT 0,
    in_stock BOOLEAN DEFAULT true,
    is_featured BOOLEAN DEFAULT false,
    is_new BOOLEAN DEFAULT false,
    tags TEXT[] DEFAULT ARRAY[]::TEXT[],
    brand TEXT DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

-- Add indexes for better query performance
CREATE INDEX IF NOT EXISTS products_category_idx ON products(category);
CREATE INDEX IF NOT EXISTS products_is_featured_idx ON products(is_featured);
CREATE INDEX IF NOT EXISTS products_is_new_idx ON products(is_new);
CREATE INDEX IF NOT EXISTS products_created_at_idx ON products(created_at DESC);
CREATE INDEX IF NOT EXISTS products_name_search_idx ON products USING gin(to_tsvector('english', name));

COMMENT ON TABLE products IS 'Product catalog for NextGenEcommerce app';
COMMENT ON COLUMN products.id IS 'Unique product identifier';
COMMENT ON COLUMN products.name IS 'Product name';
COMMENT ON COLUMN products.description IS 'Product description';
COMMENT ON COLUMN products.price IS 'Current selling price';
COMMENT ON COLUMN products.original_price IS 'Original price before discount';
COMMENT ON COLUMN products.category IS 'Product category (CLOTHING, SHOES, etc.)';
COMMENT ON COLUMN products.image_url IS 'Main product image URL from Supabase Storage';
COMMENT ON COLUMN products.images IS 'Array of additional image URLs';

-- ============================================
-- 2. ENABLE ROW LEVEL SECURITY
-- ============================================

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- ============================================
-- 3. CREATE RLS POLICIES FOR PRODUCTS TABLE
-- ============================================

-- Drop existing policies if they exist (for re-running script)
DROP POLICY IF EXISTS "Products are viewable by everyone" ON products;
DROP POLICY IF EXISTS "Only admins can insert products" ON products;
DROP POLICY IF EXISTS "Only admins can update products" ON products;
DROP POLICY IF EXISTS "Only admins can delete products" ON products;
DROP POLICY IF EXISTS "Anon users can view products" ON products;

-- Policy: Anyone can view products (including anonymous users)
CREATE POLICY "Products are viewable by everyone"
ON products FOR SELECT
TO public
USING (true);

-- Policy: Authenticated users can insert products (for admin use)
-- In production, you'd want to check for admin role
CREATE POLICY "Authenticated users can insert products"
ON products FOR INSERT
TO authenticated
WITH CHECK (true);

-- Policy: Authenticated users can update products
CREATE POLICY "Authenticated users can update products"
ON products FOR UPDATE
TO authenticated
USING (true)
WITH CHECK (true);

-- Policy: Authenticated users can delete products
CREATE POLICY "Authenticated users can delete products"
ON products FOR DELETE
TO authenticated
USING (true);

-- ============================================
-- 4. STORAGE POLICIES FOR PRODUCT-IMAGES BUCKET
-- ============================================

-- Drop existing storage policies if they exist
DROP POLICY IF EXISTS "Public product images read access" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload product images" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update product images" ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete product images" ON storage.objects;

-- Policy: Allow public read access to product images
CREATE POLICY "Public product images read access"
ON storage.objects FOR SELECT
TO public
USING (bucket_id = 'product-images');

-- Policy: Allow authenticated users to upload product images
CREATE POLICY "Authenticated users can upload product images"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (bucket_id = 'product-images');

-- Policy: Allow authenticated users to update product images
CREATE POLICY "Authenticated users can update product images"
ON storage.objects FOR UPDATE
TO authenticated
USING (bucket_id = 'product-images')
WITH CHECK (bucket_id = 'product-images');

-- Policy: Allow authenticated users to delete product images
CREATE POLICY "Authenticated users can delete product images"
ON storage.objects FOR DELETE
TO authenticated
USING (bucket_id = 'product-images');

-- ============================================
-- 5. CREATE TRIGGER FOR UPDATED_AT
-- ============================================

-- Create or replace function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_products_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger on products table
DROP TRIGGER IF EXISTS update_products_timestamp ON products;
CREATE TRIGGER update_products_timestamp
    BEFORE UPDATE ON products
    FOR EACH ROW
    EXECUTE FUNCTION update_products_updated_at();

-- ============================================
-- 6. VERIFY SETUP
-- ============================================

-- Check if products table exists
SELECT
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'public' AND table_name = 'products';

-- Check products table columns
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'products'
ORDER BY ordinal_position;

-- Check RLS is enabled
SELECT
    schemaname,
    tablename,
    rowsecurity
FROM pg_tables
WHERE tablename = 'products';

-- Check policies
SELECT
    policyname,
    tablename,
    cmd,
    roles
FROM pg_policies
WHERE tablename = 'products';

-- ============================================
-- SETUP COMPLETE!
-- ============================================

-- Product image URLs will be in format:
-- https://ccrscwaixfmfglylcjpj.supabase.co/storage/v1/object/public/product-images/{filename}
