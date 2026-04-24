-- NextGenEcommerce - Supabase Database Schema for Pakistani Payment System
-- Run this script in your Supabase SQL Editor

-- ============================================
-- 1. SHIPPING ADDRESSES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS shipping_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    label TEXT NOT NULL,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city TEXT NOT NULL,
    province TEXT NOT NULL,
    postal_code TEXT NOT NULL,
    country TEXT NOT NULL DEFAULT 'Pakistan',
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipping_addresses_user_id ON shipping_addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_shipping_addresses_is_default ON shipping_addresses(user_id, is_default);

-- Trigger to ensure only one default address per user
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
FOR EACH ROW
EXECUTE FUNCTION ensure_single_default_address();

-- ============================================
-- 2. ORDERS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    order_number TEXT UNIQUE NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL DEFAULT 0,
    shipping DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'PKR',
    status TEXT NOT NULL DEFAULT 'PENDING',
    payment_status TEXT NOT NULL DEFAULT 'PENDING',
    payment_method TEXT NOT NULL,
    shipping_address_id UUID REFERENCES shipping_addresses(id),
    tracking_number TEXT,
    estimated_delivery BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT valid_status CHECK (status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED', 'RETURNED')),
    CONSTRAINT valid_payment_status CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY'))
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_order_number ON orders(order_number);

-- ============================================
-- 3. ORDER ITEMS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL,
    product_name TEXT NOT NULL,
    product_image TEXT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    selected_size TEXT,
    selected_color TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_order_items_product_id ON order_items(product_id);

-- ============================================
-- 4. PAYMENT TRANSACTIONS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    payment_method TEXT NOT NULL,
    transaction_id TEXT UNIQUE NOT NULL,
    mobile_number TEXT,
    external_transaction_id TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    amount DECIMAL(10,2) NOT NULL,
    currency TEXT NOT NULL DEFAULT 'PKR',
    otp_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT valid_payment_method CHECK (payment_method IN ('EASYPAISA', 'JAZZCASH', 'CASH_ON_DELIVERY')),
    CONSTRAINT valid_transaction_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_payment_transactions_order_id ON payment_transactions(order_id);
CREATE INDEX IF NOT EXISTS idx_payment_transactions_transaction_id ON payment_transactions(transaction_id);

-- ============================================
-- 5. ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================

-- Enable RLS on all tables
ALTER TABLE shipping_addresses ENABLE ROW LEVEL SECURITY;
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;

-- ============================================
-- SHIPPING ADDRESSES POLICIES
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own addresses" ON shipping_addresses;
DROP POLICY IF EXISTS "Users can create own addresses" ON shipping_addresses;
DROP POLICY IF EXISTS "Users can update own addresses" ON shipping_addresses;
DROP POLICY IF EXISTS "Users can delete own addresses" ON shipping_addresses;

-- Users can view their own addresses
CREATE POLICY "Users can view own addresses"
ON shipping_addresses FOR SELECT
USING (auth.uid() = user_id);

-- Users can create their own addresses
CREATE POLICY "Users can create own addresses"
ON shipping_addresses FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Users can update their own addresses
CREATE POLICY "Users can update own addresses"
ON shipping_addresses FOR UPDATE
USING (auth.uid() = user_id);

-- Users can delete their own addresses
CREATE POLICY "Users can delete own addresses"
ON shipping_addresses FOR DELETE
USING (auth.uid() = user_id);

-- ============================================
-- ORDERS POLICIES
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own orders" ON orders;
DROP POLICY IF EXISTS "Users can create own orders" ON orders;
DROP POLICY IF EXISTS "Admins can update orders" ON orders;

-- Users can view their own orders, admins can view all
CREATE POLICY "Users can view own orders"
ON orders FOR SELECT
USING (
    auth.uid() = user_id
    OR EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- Users can create their own orders
CREATE POLICY "Users can create own orders"
ON orders FOR INSERT
WITH CHECK (auth.uid() = user_id);

-- Admins can update orders
CREATE POLICY "Admins can update orders"
ON orders FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- ============================================
-- ORDER ITEMS POLICIES
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own order items" ON order_items;
DROP POLICY IF EXISTS "Users can create order items" ON order_items;

-- Users can view items for their orders
CREATE POLICY "Users can view own order items"
ON order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = order_items.order_id
        AND orders.user_id = auth.uid()
    )
);

-- Users can create order items for their orders
CREATE POLICY "Users can create order items"
ON order_items FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = order_items.order_id
        AND orders.user_id = auth.uid()
    )
);

-- ============================================
-- PAYMENT TRANSACTIONS POLICIES
-- ============================================

-- Drop existing policies if they exist
DROP POLICY IF EXISTS "Users can view own payment transactions" ON payment_transactions;
DROP POLICY IF EXISTS "Users can create payment transactions" ON payment_transactions;

-- Users can view their own payment transactions, admins can view all
CREATE POLICY "Users can view own payment transactions"
ON payment_transactions FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = payment_transactions.order_id
        AND orders.user_id = auth.uid()
    )
    OR EXISTS (
        SELECT 1 FROM users
        WHERE id = auth.uid() AND role = 'admin'
    )
);

-- Users can create payment transactions for their orders
CREATE POLICY "Users can create payment transactions"
ON payment_transactions FOR INSERT
WITH CHECK (
    EXISTS (
        SELECT 1 FROM orders
        WHERE orders.id = payment_transactions.order_id
        AND orders.user_id = auth.uid()
    )
);

-- ============================================
-- VERIFICATION QUERIES
-- ============================================

-- Run these queries to verify the schema was created correctly:

-- Check if tables exist
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
AND table_name IN ('shipping_addresses', 'orders', 'order_items', 'payment_transactions');

-- Check if indexes exist
SELECT indexname, tablename
FROM pg_indexes
WHERE schemaname = 'public'
AND tablename IN ('shipping_addresses', 'orders', 'order_items', 'payment_transactions');

-- Check if RLS is enabled
SELECT tablename, rowsecurity
FROM pg_tables
WHERE schemaname = 'public'
AND tablename IN ('shipping_addresses', 'orders', 'order_items', 'payment_transactions');

-- Check policies
SELECT schemaname, tablename, policyname
FROM pg_policies
WHERE tablename IN ('shipping_addresses', 'orders', 'order_items', 'payment_transactions');
