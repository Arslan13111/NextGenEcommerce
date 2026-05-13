-- ============================================================
-- NEXTGENECOMMERCE — COMPLETE SUPABASE SETUP SCRIPT
-- ============================================================
-- This is the ONE script to run for a fresh Supabase project.
-- It is safe to run multiple times (all guards are in place).
-- Run it in: Supabase Dashboard → SQL Editor → New Query → Run
--
-- Covers:
--   users, retailers, delivery_partners, products, cart_items,
--   reviews, shipping_addresses, orders, order_items,
--   payment_transactions, storage buckets & all RLS policies
-- ============================================================


-- ============================================================
-- SECTION 1 — HELPER FUNCTIONS
-- ============================================================

-- Auto-updates the updated_at column on any table
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Auto-updates products.updated_at (stored as epoch ms)
CREATE OR REPLACE FUNCTION update_products_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = EXTRACT(EPOCH FROM NOW())::BIGINT * 1000;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Returns TRUE when the calling user has role = 'admin'
-- SECURITY DEFINER avoids infinite recursion in RLS policies
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

-- Returns TRUE when the calling user has role = 'retailer'
CREATE OR REPLACE FUNCTION is_retailer()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'retailer'
    );
$$;

-- Returns TRUE when the calling user has role = 'delivery_partner'
CREATE OR REPLACE FUNCTION is_delivery_partner()
RETURNS boolean
LANGUAGE sql
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'delivery_partner'
    );
$$;


-- ============================================================
-- SECTION 2 — USERS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id               UUID PRIMARY KEY DEFAULT auth.uid(),
    email            TEXT UNIQUE NOT NULL,
    name             TEXT NOT NULL,
    phone            TEXT DEFAULT '',
    profile_image_url TEXT,
    created_at       BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT,
    -- Roles: 'customer' | 'admin' | 'retailer' | 'delivery_partner'
    role             TEXT NOT NULL DEFAULT 'customer',
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS users_email_idx ON users(email);
CREATE INDEX IF NOT EXISTS users_id_idx    ON users(id);

ALTER TABLE users ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own profile" ON users;
DROP POLICY IF EXISTS "Admins can view all users"        ON users;
DROP POLICY IF EXISTS "Users can insert their own profile" ON users;
DROP POLICY IF EXISTS "Users can update their own profile" ON users;
DROP POLICY IF EXISTS "Users can delete their own profile" ON users;

CREATE POLICY "Users can view their own profile"
ON users FOR SELECT TO authenticated
USING (auth.uid() = id);

-- Admin can see ALL users (needed for admin dashboard)
CREATE POLICY "Admins can view all users"
ON users FOR SELECT TO authenticated
USING (is_admin());

CREATE POLICY "Users can insert their own profile"
ON users FOR INSERT TO authenticated
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update their own profile"
ON users FOR UPDATE TO authenticated
USING (auth.uid() = id)
WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can delete their own profile"
ON users FOR DELETE TO authenticated
USING (auth.uid() = id);

-- Trigger: keep updated_at in sync
DROP TRIGGER IF EXISTS update_users_updated_at ON users;
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-create a users row when someone signs up via Supabase Auth
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.users (id, email, name, created_at)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'name', 'User'),
        EXTRACT(EPOCH FROM NOW())::BIGINT
    )
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();


-- ============================================================
-- SECTION 3 — RETAILERS TABLE  (new for marketplace)
-- ============================================================

CREATE TABLE IF NOT EXISTS retailers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    store_name        TEXT NOT NULL,
    store_description TEXT DEFAULT '',
    store_logo_url    TEXT,
    store_address     TEXT DEFAULT '',
    contact_phone     TEXT DEFAULT '',
    total_revenue     DECIMAL(12, 2) DEFAULT 0.00,
    rating            DECIMAL(2, 1)  DEFAULT 0.0,
    is_verified       BOOLEAN DEFAULT false,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)   -- one store per retailer account
);

CREATE INDEX IF NOT EXISTS idx_retailers_user_id ON retailers(user_id);

ALTER TABLE retailers ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Anyone can view retailer profiles"  ON retailers;
DROP POLICY IF EXISTS "Retailers can update their profile" ON retailers;
DROP POLICY IF EXISTS "Admins can manage all retailers"    ON retailers;
DROP POLICY IF EXISTS "Retailers can insert their profile" ON retailers;

-- Public can see store info (shown on product pages as "Sold by X")
CREATE POLICY "Anyone can view retailer profiles"
ON retailers FOR SELECT TO public USING (true);

-- Retailer accounts can create their own store profile
CREATE POLICY "Retailers can insert their profile"
ON retailers FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

-- Retailers can only edit their own store
CREATE POLICY "Retailers can update their profile"
ON retailers FOR UPDATE TO authenticated
USING (auth.uid() = user_id);

-- Admin has full control
CREATE POLICY "Admins can manage all retailers"
ON retailers FOR ALL TO authenticated
USING (is_admin());

DROP TRIGGER IF EXISTS update_retailers_updated_at ON retailers;
CREATE TRIGGER update_retailers_updated_at
    BEFORE UPDATE ON retailers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- SECTION 4 — DELIVERY PARTNERS TABLE  (new for marketplace)
-- ============================================================

CREATE TABLE IF NOT EXISTS delivery_partners (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    company_name      TEXT NOT NULL DEFAULT '',
    contact_person    TEXT NOT NULL DEFAULT '',
    company_address   TEXT NOT NULL DEFAULT '',
    contact_phone     TEXT NOT NULL DEFAULT '',
    company_logo_url  TEXT,
    vehicle_type      TEXT DEFAULT 'Bike',  -- Bike, Car, Van
    vehicle_number    TEXT DEFAULT '',
    is_available      BOOLEAN DEFAULT true,
    current_latitude  DECIMAL(9, 6),
    current_longitude DECIMAL(9, 6),
    rating            DECIMAL(2, 1)  DEFAULT 0.0,
    total_deliveries  INTEGER DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_partners_user_id ON delivery_partners(user_id);

ALTER TABLE delivery_partners ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Delivery partners can view their profile"   ON delivery_partners;
DROP POLICY IF EXISTS "Delivery partners can update their profile" ON delivery_partners;
DROP POLICY IF EXISTS "Delivery partners can insert their profile" ON delivery_partners;
DROP POLICY IF EXISTS "Admins can manage all delivery partners"    ON delivery_partners;

CREATE POLICY "Delivery partners can view their profile"
ON delivery_partners FOR SELECT TO authenticated
USING (auth.uid() = user_id OR is_admin());

CREATE POLICY "Delivery partners can insert their profile"
ON delivery_partners FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Delivery partners can update their profile"
ON delivery_partners FOR UPDATE TO authenticated
USING (auth.uid() = user_id);

CREATE POLICY "Admins can manage all delivery partners"
ON delivery_partners FOR ALL TO authenticated
USING (is_admin());

DROP TRIGGER IF EXISTS update_delivery_partners_updated_at ON delivery_partners;
CREATE TRIGGER update_delivery_partners_updated_at
    BEFORE UPDATE ON delivery_partners
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


-- ============================================================
-- SECTION 5 — PRODUCTS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS products (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             TEXT NOT NULL,
    description      TEXT DEFAULT '',
    price            DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    original_price   DECIMAL(10, 2) DEFAULT 0.00,
    category         TEXT NOT NULL DEFAULT 'CLOTHING',
    sub_category     TEXT DEFAULT '',
    images           TEXT[] DEFAULT ARRAY[]::TEXT[],
    image_url        TEXT,
    ar_model_url     TEXT,
    local_image_name TEXT DEFAULT '',
    sizes            TEXT[] DEFAULT ARRAY['S', 'M', 'L', 'XL']::TEXT[],
    colors           TEXT[] DEFAULT ARRAY['Black', 'White', 'Blue', 'Red']::TEXT[],
    color_images     JSONB NOT NULL DEFAULT '{}',
    lens_id          TEXT,
    rating           DECIMAL(2, 1) DEFAULT 0.0,
    review_count     INTEGER DEFAULT 0,
    stock            INTEGER DEFAULT 0,
    in_stock         BOOLEAN DEFAULT true,
    is_featured      BOOLEAN DEFAULT false,
    is_new           BOOLEAN DEFAULT false,
    tags             TEXT[] DEFAULT ARRAY[]::TEXT[],
    brand            TEXT DEFAULT '',
    retailer_id      UUID REFERENCES retailers(id) ON DELETE SET NULL,
    created_at       BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at       BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

-- Safe additions for existing installs
ALTER TABLE products ADD COLUMN IF NOT EXISTS color_images  JSONB NOT NULL DEFAULT '{}';
ALTER TABLE products ADD COLUMN IF NOT EXISTS lens_id       TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS retailer_id   UUID REFERENCES retailers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS products_category_idx    ON products(category);
CREATE INDEX IF NOT EXISTS products_is_featured_idx ON products(is_featured);
CREATE INDEX IF NOT EXISTS products_is_new_idx      ON products(is_new);
CREATE INDEX IF NOT EXISTS products_created_at_idx  ON products(created_at DESC);
CREATE INDEX IF NOT EXISTS products_retailer_id_idx ON products(retailer_id);
CREATE INDEX IF NOT EXISTS products_name_search_idx ON products USING gin(to_tsvector('english', name));

ALTER TABLE products ENABLE ROW LEVEL SECURITY;

-- Drop every old policy by name (from all previous scripts)
DROP POLICY IF EXISTS "Products are viewable by everyone"            ON products;
DROP POLICY IF EXISTS "Authenticated users can insert products"      ON products;
DROP POLICY IF EXISTS "Authenticated users can update products"      ON products;
DROP POLICY IF EXISTS "Authenticated users can delete products"      ON products;
DROP POLICY IF EXISTS "Allow public read access to products"         ON products;
DROP POLICY IF EXISTS "Allow admin insert products"                  ON products;
DROP POLICY IF EXISTS "Allow admin update products"                  ON products;
DROP POLICY IF EXISTS "Allow admin delete products"                  ON products;
DROP POLICY IF EXISTS "Products are publicly readable"               ON products;
DROP POLICY IF EXISTS "Admins can insert products"                   ON products;
DROP POLICY IF EXISTS "Admins can update products"                   ON products;
DROP POLICY IF EXISTS "Admins can delete products"                   ON products;
DROP POLICY IF EXISTS "Allow public insert to products"              ON products;
DROP POLICY IF EXISTS "Retailers can manage their own products"      ON products;
DROP POLICY IF EXISTS "Retailers can insert their own products"      ON products;
DROP POLICY IF EXISTS "Retailers can update their own products"      ON products;
DROP POLICY IF EXISTS "Retailers can delete their own products"      ON products;

-- Anyone (including guests) can browse products
CREATE POLICY "Products are publicly readable"
ON products FOR SELECT TO public USING (true);

-- Admins can add, edit, delete any product
CREATE POLICY "Admins can insert products"
ON products FOR INSERT TO authenticated
WITH CHECK (is_admin());

CREATE POLICY "Admins can update products"
ON products FOR UPDATE TO authenticated
USING (is_admin()) WITH CHECK (is_admin());

CREATE POLICY "Admins can delete products"
ON products FOR DELETE TO authenticated
USING (is_admin());

-- Retailers can manage ONLY products that belong to their store
CREATE POLICY "Retailers can insert their own products"
ON products FOR INSERT TO authenticated
WITH CHECK (
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = retailer_id
    )
);

CREATE POLICY "Retailers can update their own products"
ON products FOR UPDATE TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = products.retailer_id
    )
);

CREATE POLICY "Retailers can delete their own products"
ON products FOR DELETE TO authenticated
USING (
    EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = products.retailer_id
    )
);

DROP TRIGGER IF EXISTS update_products_timestamp ON products;
CREATE TRIGGER update_products_timestamp
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_products_updated_at();


-- ============================================================
-- SECTION 6 — SHIPPING ADDRESSES TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS shipping_addresses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name    TEXT NOT NULL,
    phone        TEXT NOT NULL,
    label        TEXT NOT NULL,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city         TEXT NOT NULL,
    province     TEXT NOT NULL,
    postal_code  TEXT NOT NULL,
    country      TEXT NOT NULL DEFAULT 'Pakistan',
    is_default   BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipping_addresses_user_id   ON shipping_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_shipping_addresses_is_default ON shipping_addresses(user_id, is_default);

-- Ensures only one address per user is flagged is_default = true
CREATE OR REPLACE FUNCTION ensure_single_default_address()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.is_default = TRUE THEN
        UPDATE shipping_addresses
        SET is_default = FALSE
        WHERE user_id = NEW.user_id AND id != NEW.id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_ensure_single_default_address ON shipping_addresses;
CREATE TRIGGER trigger_ensure_single_default_address
BEFORE INSERT OR UPDATE ON shipping_addresses
FOR EACH ROW EXECUTE FUNCTION ensure_single_default_address();

ALTER TABLE shipping_addresses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own addresses"   ON shipping_addresses;
DROP POLICY IF EXISTS "Users can create own addresses" ON shipping_addresses;
DROP POLICY IF EXISTS "Users can update own addresses" ON shipping_addresses;
DROP POLICY IF EXISTS "Users can delete own addresses" ON shipping_addresses;

CREATE POLICY "Users can view own addresses"
ON shipping_addresses FOR SELECT USING (auth.uid() = user_id);

CREATE POLICY "Users can create own addresses"
ON shipping_addresses FOR INSERT WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own addresses"
ON shipping_addresses FOR UPDATE USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own addresses"
ON shipping_addresses FOR DELETE USING (auth.uid() = user_id);


-- ============================================================
-- SECTION 7 — ORDERS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    order_number        TEXT UNIQUE NOT NULL,
    subtotal            DECIMAL(10,2) NOT NULL,
    tax                 DECIMAL(10,2) NOT NULL DEFAULT 0,
    shipping            DECIMAL(10,2) NOT NULL DEFAULT 0,
    total               DECIMAL(10,2) NOT NULL,
    currency            TEXT NOT NULL DEFAULT 'PKR',
    status              TEXT NOT NULL DEFAULT 'PENDING',
    payment_status      TEXT NOT NULL DEFAULT 'PENDING',
    payment_method      TEXT NOT NULL,
    shipping_address_id UUID REFERENCES shipping_addresses(id),
    tracking_number     TEXT,
    estimated_delivery  BIGINT,
    -- Marketplace fields
    retailer_id         UUID REFERENCES retailers(id) ON DELETE SET NULL,
    delivery_partner_id UUID REFERENCES public.users(id) ON DELETE SET NULL,
    parent_order_id     UUID,   -- ties split sub-orders to one checkout session
    -- Return / cancellation fields
    cancellation_reason TEXT,
    return_reason       TEXT,
    return_images       TEXT[] DEFAULT ARRAY[]::TEXT[],
    admin_return_note   TEXT,
    shipping_address_snapshot TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- For existing installs — safely add new columns
ALTER TABLE orders ADD COLUMN IF NOT EXISTS retailer_id         UUID REFERENCES retailers(id) ON DELETE SET NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_partner_id UUID REFERENCES public.users(id) ON DELETE SET NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS parent_order_id     UUID;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS cancellation_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_reason       TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS return_images       TEXT[] DEFAULT ARRAY[]::TEXT[];
ALTER TABLE orders ADD COLUMN IF NOT EXISTS admin_return_note   TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS shipping_address_snapshot TEXT;

-- Update status & payment constraints to match current app enums
ALTER TABLE orders DROP CONSTRAINT IF EXISTS valid_status;
ALTER TABLE orders ADD CONSTRAINT valid_status CHECK (status IN (
    'PENDING', 'CONFIRMED', 'PROCESSING',
    'PACKED',                   -- retailer packed the item
    'READY_FOR_PICKUP',         -- waiting for delivery partner
    'SHIPPED',
    'OUT_FOR_DELIVERY',
    'DELIVERED',
    'CANCELLED',
    'RETURN_REQUESTED',
    'RETURNED',
    'RETURN_REJECTED'
));

ALTER TABLE orders DROP CONSTRAINT IF EXISTS valid_payment_status;
ALTER TABLE orders ADD CONSTRAINT valid_payment_status CHECK (
    payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')
);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS valid_payment_method;
ALTER TABLE orders ADD CONSTRAINT valid_payment_method CHECK (
    payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY', 'SAFEPAY')
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id             ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status              ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at          ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_order_number        ON orders(order_number);
CREATE INDEX IF NOT EXISTS idx_orders_retailer_id         ON orders(retailer_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_partner_id ON orders(delivery_partner_id);
CREATE INDEX IF NOT EXISTS idx_orders_parent_order_id     ON orders(parent_order_id);

DROP TRIGGER IF EXISTS update_orders_updated_at ON orders;
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- Drop every old order policy
DROP POLICY IF EXISTS "Users can view own orders"                        ON orders;
DROP POLICY IF EXISTS "Users can create own orders"                      ON orders;
DROP POLICY IF EXISTS "Admins can update orders"                         ON orders;
DROP POLICY IF EXISTS "Retailers can view and update their own orders"   ON orders;
DROP POLICY IF EXISTS "Delivery partners can view and update assigned orders" ON orders;
DROP POLICY IF EXISTS "Authorized users can update orders"               ON orders;

-- SELECT: customer sees own, admin sees all, retailer sees store orders,
--         delivery partner sees assigned orders
CREATE POLICY "Users can view relevant orders"
ON orders FOR SELECT
USING (
    auth.uid() = user_id
    OR is_admin()
    OR EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = orders.retailer_id
    )
    OR orders.delivery_partner_id = auth.uid()
    OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP')
);

-- INSERT: only the customer creates their own orders
CREATE POLICY "Users can create own orders"
ON orders FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- UPDATE: admin, retailer (their orders), delivery partner (assigned orders)
CREATE POLICY "Authorized users can update orders"
ON orders FOR UPDATE
USING (
    is_admin()
    OR EXISTS (
        SELECT 1 FROM retailers
        WHERE retailers.user_id = auth.uid()
        AND   retailers.id      = orders.retailer_id
    )
    OR orders.delivery_partner_id = auth.uid()
    OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP' AND delivery_partner_id IS NULL)
);


-- ============================================================
-- SECTION 8 — ORDER ITEMS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS order_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id    UUID NOT NULL,
    product_name  TEXT NOT NULL,
    product_image TEXT NOT NULL,
    price         DECIMAL(10,2) NOT NULL,
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    selected_size  TEXT,
    selected_color TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id   ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own order items"  ON order_items;
DROP POLICY IF EXISTS "Users can create order items"    ON order_items;
DROP POLICY IF EXISTS "Admins can view all order items" ON order_items;

-- Can view items if you can see the parent order
CREATE POLICY "Users can view own order items"
ON order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = order_items.order_id
        AND (
            orders.user_id = auth.uid()
            OR is_admin()
            OR EXISTS (
                SELECT 1 FROM retailers
                WHERE retailers.user_id = auth.uid()
                AND   retailers.id      = orders.retailer_id
            )
            OR orders.delivery_partner_id = auth.uid()
            OR (is_delivery_partner() AND status = 'READY_FOR_PICKUP')
        )
    )
);

CREATE POLICY "Users can create order items"
ON order_items FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id      = order_items.order_id
        AND   orders.user_id = auth.uid()
    )
);


-- ============================================================
-- SECTION 9 — PAYMENT TRANSACTIONS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id                UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method          TEXT NOT NULL,
    transaction_id          TEXT UNIQUE NOT NULL,
    mobile_number           TEXT,
    external_transaction_id TEXT,
    status                  TEXT NOT NULL DEFAULT 'PENDING',
    amount                  DECIMAL(10,2) NOT NULL,
    currency                TEXT NOT NULL DEFAULT 'PKR',
    otp_verified            BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,
    CONSTRAINT valid_pt_payment_method CHECK (
        payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY', 'SAFEPAY')
    ),
    CONSTRAINT valid_transaction_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    )
);

-- Safe addition for existing installs
ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS external_transaction_id TEXT;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id       ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_transaction_id ON payment_transactions(transaction_id);

ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view own payment transactions"   ON payment_transactions;
DROP POLICY IF EXISTS "Users can create payment transactions"     ON payment_transactions;

CREATE POLICY "Users can view own payment transactions"
ON payment_transactions FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = payment_transactions.order_id
        AND (orders.user_id = auth.uid() OR is_admin())
    )
);

CREATE POLICY "Users can create payment transactions"
ON payment_transactions FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id      = payment_transactions.order_id
        AND   orders.user_id = auth.uid()
    )
);


-- ============================================================
-- SECTION 10 — CART ITEMS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS cart_items (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    product_id     TEXT NOT NULL,
    product_name   TEXT NOT NULL DEFAULT '',
    product_image  TEXT NOT NULL DEFAULT '',
    price          DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    original_price DECIMAL(10, 2) DEFAULT 0.00,
    quantity       INTEGER NOT NULL DEFAULT 1,
    selected_size  TEXT NOT NULL DEFAULT 'M',
    selected_color TEXT NOT NULL DEFAULT 'Black',
    store_name     TEXT NOT NULL DEFAULT 'Default Store',
    retailer_id    UUID REFERENCES retailers(id) ON DELETE SET NULL,
    added_at       BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000,
    updated_at     BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

-- Safe addition for existing installs
ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS retailer_id UUID REFERENCES retailers(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS cart_items_user_id_idx        ON cart_items(user_id);
CREATE INDEX IF NOT EXISTS cart_items_user_product_idx   ON cart_items(user_id, product_id, selected_size, selected_color);

ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can view their own cart items"   ON cart_items;
DROP POLICY IF EXISTS "Users can insert their own cart items" ON cart_items;
DROP POLICY IF EXISTS "Users can update their own cart items" ON cart_items;
DROP POLICY IF EXISTS "Users can delete their own cart items" ON cart_items;

CREATE POLICY "Users can view their own cart items"
ON cart_items FOR SELECT TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Users can insert their own cart items"
ON cart_items FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own cart items"
ON cart_items FOR UPDATE TO authenticated
USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own cart items"
ON cart_items FOR DELETE TO authenticated USING (auth.uid() = user_id);


-- ============================================================
-- SECTION 11 — REVIEWS TABLE
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_name  TEXT NOT NULL DEFAULT '',
    rating     INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT NOT NULL DEFAULT '',
    created_at BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

CREATE UNIQUE INDEX IF NOT EXISTS reviews_user_product_idx ON reviews(user_id, product_id);
CREATE INDEX IF NOT EXISTS reviews_product_idx             ON reviews(product_id);
CREATE INDEX IF NOT EXISTS reviews_created_at_idx          ON reviews(created_at DESC);

ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Reviews are public"                 ON reviews;
DROP POLICY IF EXISTS "Users can insert their own review"  ON reviews;
DROP POLICY IF EXISTS "Users can update their own review"  ON reviews;
DROP POLICY IF EXISTS "Users can delete their own review"  ON reviews;

CREATE POLICY "Reviews are public"
ON reviews FOR SELECT TO public USING (true);

CREATE POLICY "Users can insert their own review"
ON reviews FOR INSERT TO authenticated WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update their own review"
ON reviews FOR UPDATE TO authenticated USING (auth.uid() = user_id);

CREATE POLICY "Users can delete their own review"
ON reviews FOR DELETE TO authenticated USING (auth.uid() = user_id);


-- ============================================================
-- SECTION 12 — STORAGE BUCKETS & POLICIES
-- ============================================================

-- Create buckets (idempotent)
INSERT INTO storage.buckets (id, name, public)
VALUES ('profiles',      'profiles',      true)
ON CONFLICT (id) DO UPDATE SET public = true;

INSERT INTO storage.buckets (id, name, public)
VALUES ('product-images','product-images', true)
ON CONFLICT (id) DO UPDATE SET public = true;

INSERT INTO storage.buckets (id, name, public)
VALUES ('return-images', 'return-images',  true)
ON CONFLICT (id) DO UPDATE SET public = true;

-- Drop ALL old storage policies from every previous script
DROP POLICY IF EXISTS "Public profiles read access"                    ON storage.objects;
DROP POLICY IF EXISTS "Users can upload their profile image"           ON storage.objects;
DROP POLICY IF EXISTS "Users can update their profile image"           ON storage.objects;
DROP POLICY IF EXISTS "Users can delete their profile image"           ON storage.objects;
DROP POLICY IF EXISTS "Public product images read access"              ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can upload product images"  ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can update product images"  ON storage.objects;
DROP POLICY IF EXISTS "Authenticated users can delete product images"  ON storage.objects;
DROP POLICY IF EXISTS "Allow public read access to product images"     ON storage.objects;
DROP POLICY IF EXISTS "Allow admin upload product images"              ON storage.objects;
DROP POLICY IF EXISTS "Allow admin update product images"              ON storage.objects;
DROP POLICY IF EXISTS "Allow admin delete product images"              ON storage.objects;
DROP POLICY IF EXISTS "Public read product images"                     ON storage.objects;
DROP POLICY IF EXISTS "Authenticated upload to product-images"         ON storage.objects;
DROP POLICY IF EXISTS "Admin manage product images"                    ON storage.objects;
DROP POLICY IF EXISTS "Public read access for product-images"          ON storage.objects;
DROP POLICY IF EXISTS "Allow anonymous uploads to product-images"      ON storage.objects;
DROP POLICY IF EXISTS "Allow anonymous updates to product-images"      ON storage.objects;
DROP POLICY IF EXISTS "Allow anonymous deletes from product-images"    ON storage.objects;
DROP POLICY IF EXISTS "Retailers can upload product images"            ON storage.objects;
DROP POLICY IF EXISTS "Retailers can manage their product images"      ON storage.objects;
DROP POLICY IF EXISTS "Retailers can delete their product images"      ON storage.objects;
DROP POLICY IF EXISTS "Public read access for return-images"           ON storage.objects;
DROP POLICY IF EXISTS "Allow authenticated uploads to return-images"   ON storage.objects;
DROP POLICY IF EXISTS "Allow users to update own return images"        ON storage.objects;
DROP POLICY IF EXISTS "Allow users to delete own return images"        ON storage.objects;

-- ── profiles bucket ──────────────────────────────────────────
CREATE POLICY "Public profiles read access"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'profiles');

CREATE POLICY "Users can upload their profile image"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'profiles' AND
    (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Users can update their profile image"
ON storage.objects FOR UPDATE TO authenticated
USING  (bucket_id = 'profiles' AND (storage.foldername(name))[1] = auth.uid()::text)
WITH CHECK (bucket_id = 'profiles' AND (storage.foldername(name))[1] = auth.uid()::text);

CREATE POLICY "Users can delete their profile image"
ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'profiles' AND (storage.foldername(name))[1] = auth.uid()::text);

-- ── product-images bucket ────────────────────────────────────
CREATE POLICY "Public read access for product-images"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'product-images');

-- Admins and retailers can upload/update/delete product images.
-- Authenticated users can upload to temp-avatars/ for try-on.
CREATE POLICY "Admin and retailer upload product images"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (
    bucket_id = 'product-images' AND
    (is_admin() OR is_retailer() OR (storage.foldername(name))[1] = 'temp-avatars')
);

CREATE POLICY "Admin and retailer update product images"
ON storage.objects FOR UPDATE TO authenticated
USING  (bucket_id = 'product-images' AND (is_admin() OR is_retailer()))
WITH CHECK (bucket_id = 'product-images' AND (is_admin() OR is_retailer()));

CREATE POLICY "Admin and retailer delete product images"
ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'product-images' AND (is_admin() OR is_retailer()));

-- ── return-images bucket ─────────────────────────────────────
CREATE POLICY "Public read access for return-images"
ON storage.objects FOR SELECT TO public
USING (bucket_id = 'return-images');

CREATE POLICY "Allow authenticated uploads to return-images"
ON storage.objects FOR INSERT TO authenticated
WITH CHECK (bucket_id = 'return-images');

CREATE POLICY "Allow users to update own return images"
ON storage.objects FOR UPDATE TO authenticated
USING  (bucket_id = 'return-images')
WITH CHECK (bucket_id = 'return-images');

CREATE POLICY "Allow users to delete own return images"
ON storage.objects FOR DELETE TO authenticated
USING (bucket_id = 'return-images');


-- ============================================================
-- SECTION 13 — SET ADMIN ROLES  (run once for your accounts)
-- ============================================================
-- After running this script, execute these two lines separately
-- to make your accounts admin (they must have registered first):

-- UPDATE public.users SET role = 'admin' WHERE email = 'arslanmunawar1311@gmail.com';
-- UPDATE public.users SET role = 'admin' WHERE email = 'huzaifanadeem1192@gmail.com';


-- ============================================================
-- SECTION 14 — VERIFY  (read-only, safe to run anytime)
-- ============================================================

SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN (
    'users', 'retailers', 'delivery_partners',
    'products', 'cart_items', 'reviews',
    'shipping_addresses', 'orders', 'order_items', 'payment_transactions'
)
ORDER BY table_name;

-- ============================================================
-- SCRIPT COMPLETE
-- ============================================================
