-- ============================================================
-- Run this in your Supabase SQL Editor (https://supabase.com/dashboard)
-- ============================================================

-- 1. FCM token storage (one token per user, upserted on every login)
CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id          UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    fcm_token   TEXT NOT NULL,
    role        TEXT NOT NULL,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (user_id)
);

-- 2. Push notifications log (every admin/retailer send creates a row here)
CREATE TABLE IF NOT EXISTS push_notifications (
    id              UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    title           TEXT NOT NULL,
    message         TEXT NOT NULL,
    sender_id       UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    sender_role     TEXT NOT NULL,          -- 'admin' | 'retailer'
    recipient_role  TEXT NOT NULL,          -- 'all' | 'customer' | 'retailer' | 'delivery_partner'
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Row Level Security
ALTER TABLE user_fcm_tokens  ENABLE ROW LEVEL SECURITY;
ALTER TABLE push_notifications ENABLE ROW LEVEL SECURITY;

-- user_fcm_tokens: each user manages only their own token
CREATE POLICY "Own token" ON user_fcm_tokens
    FOR ALL
    USING  (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- push_notifications: any authenticated user can insert (sender_id must be their own id)
CREATE POLICY "Insert own notifications" ON push_notifications
    FOR INSERT
    WITH CHECK (auth.uid() = sender_id);

-- push_notifications: all authenticated users can read
CREATE POLICY "Read notifications" ON push_notifications
    FOR SELECT
    USING (auth.role() = 'authenticated');

-- 4. Enable Realtime for push_notifications (needed for Supabase Realtime subscriptions)
ALTER PUBLICATION supabase_realtime ADD TABLE push_notifications;
