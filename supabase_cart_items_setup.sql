-- ============================================
-- Supabase Cart Items Table Setup Script
-- NextGenEcommerce Android App
-- ============================================

CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    product_id TEXT NOT NULL,
    product_name TEXT NOT NULL DEFAULT '',
    product_image TEXT NOT NULL DEFAULT '',
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    original_price DECIMAL(10, 2) DEFAULT 0.00,
    quantity INTEGER NOT NULL DEFAULT 1,
    selected_size TEXT NOT NULL DEFAULT 'M',
    selected_color TEXT NOT NULL DEFAULT 'Black',
    store_name TEXT NOT NULL DEFAULT 'Default Store',
    added_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

-- Indexes
CREATE INDEX IF NOT EXISTS cart_items_user_id_idx ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS cart_items_user_product_idx ON cart_items(user_id, product_id, selected_size, selected_color);

-- Enable RLS
ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;

-- RLS Policies: Users can only access their own cart items
DROP POLICY IF EXISTS "Users can view their own cart items" ON cart_items;
CREATE POLICY "Users can view their own cart items"
ON cart_items FOR SELECT
TO authenticated
USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can insert their own cart items" ON cart_items;
CREATE POLICY "Users can insert their own cart items"
ON cart_items FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can update their own cart items" ON cart_items;
CREATE POLICY "Users can update their own cart items"
ON cart_items FOR UPDATE
TO authenticated
USING (auth.uid() = user_id)
WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can delete their own cart items" ON cart_items;
CREATE POLICY "Users can delete their own cart items"
ON cart_items FOR DELETE
TO authenticated
USING (auth.uid() = user_id);
