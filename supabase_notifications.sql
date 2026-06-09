-- ============================================================
-- Notifications Setup
-- NextGenEcommerce Android App
-- Run this in Supabase SQL Editor
-- ============================================================

-- Step 1: Create push_notifications table (if not already created)
CREATE TABLE IF NOT EXISTS push_notifications (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title         TEXT NOT NULL,
    message       TEXT NOT NULL,
    sender_id     UUID,
    sender_role   TEXT NOT NULL DEFAULT 'system',
    recipient_role TEXT NOT NULL DEFAULT 'all',
    recipient_id  UUID,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

-- Step 2: Add recipient_id column if table already existed without it
ALTER TABLE push_notifications
    ADD COLUMN IF NOT EXISTS recipient_id UUID;

-- Step 3: Enable Realtime on push_notifications
-- This is the most common missing step — without it, Supabase Realtime
-- will NOT broadcast inserts to subscribed clients.
ALTER PUBLICATION supabase_realtime ADD TABLE push_notifications;

-- Step 4: RLS for push_notifications
ALTER TABLE push_notifications ENABLE ROW LEVEL SECURITY;

-- Allow any authenticated user (retailer, admin, system) to insert notifications
DROP POLICY IF EXISTS "Authenticated users can insert notifications" ON push_notifications;
CREATE POLICY "Authenticated users can insert notifications"
    ON push_notifications FOR INSERT
    TO authenticated
    WITH CHECK (true);

-- Allow customers to read notifications targeted at them or broadcast to 'customer'/'all'
DROP POLICY IF EXISTS "Users can read their own notifications" ON push_notifications;
CREATE POLICY "Users can read their own notifications"
    ON push_notifications FOR SELECT
    TO authenticated
    USING (
        recipient_role = 'all'
        OR recipient_id = auth.uid()
        OR (recipient_role = 'customer'  AND recipient_id IS NULL)
        OR (recipient_role = 'retailer'  AND recipient_id IS NULL)
        OR (recipient_role = 'delivery_partner' AND recipient_id IS NULL)
    );

-- ── FCM Token storage ─────────────────────────────────────────────────────────

-- Step 5: Create user_fcm_tokens table
CREATE TABLE IF NOT EXISTS user_fcm_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    fcm_token   TEXT NOT NULL,
    role        TEXT NOT NULL DEFAULT 'customer',
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- Step 6: RLS for user_fcm_tokens
ALTER TABLE user_fcm_tokens ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Users can manage their own FCM token" ON user_fcm_tokens;
CREATE POLICY "Users can manage their own FCM token"
    ON user_fcm_tokens FOR ALL
    TO authenticated
    USING (user_id = auth.uid())
    WITH CHECK (user_id = auth.uid());
