-- ============================================
-- Marketplace & Delivery System Setup
-- NextGenEcommerce Android App
-- ============================================

-- 1. UPDATE USER ROLES
-- Note: We add roles to the check constraint if you have one, 
-- or just rely on the application logic. 
-- In the current schema, roles are just TEXT.

-- 2. CREATE RETAILERS TABLE
CREATE TABLE IF NOT EXISTS retailers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    store_name TEXT NOT NULL,
    store_description TEXT,
    store_logo_url TEXT,
    store_address TEXT,
    contact_phone TEXT,
    total_revenue DECIMAL(12, 2) DEFAULT 0.00,
    rating DECIMAL(2, 1) DEFAULT 0.0,
    is_verified BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 3. CREATE DELIVERY PARTNERS TABLE
CREATE TABLE IF NOT EXISTS delivery_partners (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    company_name TEXT NOT NULL DEFAULT '',
    contact_person TEXT NOT NULL DEFAULT '',
    company_address TEXT NOT NULL DEFAULT '',
    contact_phone TEXT NOT NULL DEFAULT '',
    company_logo_url TEXT,
    vehicle_type TEXT,
    vehicle_number TEXT,
    is_available BOOLEAN DEFAULT true,
    current_latitude DECIMAL(9, 6),
    current_longitude DECIMAL(9, 6),
    rating DECIMAL(2, 1) DEFAULT 0.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 4. LINK PRODUCTS TO RETAILERS
-- Add retailer_id to products
ALTER TABLE products ADD COLUMN IF NOT EXISTS retailer_id UUID REFERENCES retailers(id);

-- 5. LINK ORDERS TO RETAILERS & DELIVERY
-- Add retailer_id and delivery_partner_id to orders
ALTER TABLE orders ADD COLUMN IF NOT EXISTS retailer_id UUID REFERENCES retailers(id);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_partner_id UUID REFERENCES public.users(id);

-- 6. INDEXES FOR PERFORMANCE
CREATE INDEX IF NOT EXISTS idx_products_retailer_id ON products(retailer_id);
CREATE INDEX IF NOT EXISTS idx_orders_retailer_id ON orders(retailer_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_partner_id ON orders(delivery_partner_id);

-- 7. RLS POLICIES FOR RETAILERS
ALTER TABLE retailers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view retailer profiles"
ON retailers FOR SELECT
TO public
USING (true);

CREATE POLICY "Retailers can update their own profile"
ON retailers FOR UPDATE
TO authenticated
USING (auth.uid() = user_id);

-- 8. RLS POLICIES FOR DELIVERY PARTNERS
ALTER TABLE delivery_partners ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Delivery partners can manage their own profile"
ON delivery_partners FOR ALL
TO authenticated
USING (auth.uid() = user_id);

-- 9. UPDATE PRODUCT POLICIES FOR RETAILERS
-- Existing policy was: "Authenticated users can insert products"
-- We need to make it specific to the retailer
DROP POLICY IF EXISTS "Authenticated users can insert products" ON products;
DROP POLICY IF EXISTS "Authenticated users can update products" ON products;
DROP POLICY IF EXISTS "Authenticated users can delete products" ON products;

CREATE POLICY "Retailers can manage their own products"
ON products FOR ALL
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND retailers.id = products.retailer_id
    )
    OR
    EXISTS (
        SELECT 1 FROM users
        WHERE users.id = auth.uid() AND users.role = 'admin'
    )
);

-- 10. UPDATE ORDER POLICIES
DROP POLICY IF EXISTS "Admins can update orders" ON orders;

CREATE POLICY "Retailers can view and update their own orders"
ON orders FOR ALL
TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND retailers.id = orders.retailer_id
    )
);

CREATE POLICY "Delivery partners can view and update assigned orders"
ON orders FOR ALL
TO authenticated
USING (
    orders.delivery_partner_id = auth.uid()
);

-- 11. TRIGGER FOR UPDATED_AT
CREATE TRIGGER update_retailers_updated_at
    BEFORE UPDATE ON retailers
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_delivery_partners_updated_at
    BEFORE UPDATE ON delivery_partners
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
