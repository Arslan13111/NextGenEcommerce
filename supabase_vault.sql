-- ============================================================
-- Customer Vault (Wallet) Feature
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- Step 1: Create user_wallets table
CREATE TABLE IF NOT EXISTS user_wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    balance DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Step 2: Create wallet_transactions table
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    wallet_id UUID NOT NULL REFERENCES user_wallets(id) ON DELETE CASCADE,
    amount DECIMAL(12,2) NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('CREDIT', 'DEBIT')),
    description TEXT NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Step 3: RLS for user_wallets
ALTER TABLE user_wallets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own wallet"
    ON user_wallets FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "Users can insert their own wallet"
    ON user_wallets FOR INSERT
    WITH CHECK (user_id = auth.uid());

CREATE POLICY "Users can update their own wallet"
    ON user_wallets FOR UPDATE
    USING (user_id = auth.uid());

-- Step 4: RLS for wallet_transactions
ALTER TABLE wallet_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own transactions"
    ON wallet_transactions FOR SELECT
    USING (user_id = auth.uid());

CREATE POLICY "Users can insert their own transactions"
    ON wallet_transactions FOR INSERT
    WITH CHECK (user_id = auth.uid());

-- Step 5: Allow service role (used by server-side triggers) full access
-- (Optional — only needed if you add server-side triggers later)
-- GRANT ALL ON user_wallets TO service_role;
-- GRANT ALL ON wallet_transactions TO service_role;
