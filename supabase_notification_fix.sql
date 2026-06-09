-- ============================================================
-- Notification Fix: retailer → customer status notifications
-- Run this in Supabase SQL Editor
-- ============================================================

-- 1. Add recipient_id for targeted (per-customer) notifications.
--    Without this, notifications broadcast to ALL customers.
ALTER TABLE push_notifications
ADD COLUMN IF NOT EXISTS recipient_id UUID REFERENCES auth.users(id) ON DELETE SET NULL;

-- 2. Ensure Realtime is enabled on push_notifications so the
--    Supabase Realtime listener in the app fires on INSERT.
--    (Safe to run again even if already added.)
ALTER PUBLICATION supabase_realtime ADD TABLE push_notifications;

-- 3. Allow retailers to insert notifications (sender_id = their own uid).
--    Drop and recreate to ensure correct definition.
DROP POLICY IF EXISTS "Insert own notifications" ON push_notifications;
CREATE POLICY "Insert own notifications" ON push_notifications
    FOR INSERT TO authenticated
    WITH CHECK (auth.uid() = sender_id);

-- 4. Allow every authenticated user to read notifications meant for them.
DROP POLICY IF EXISTS "Read notifications" ON push_notifications;
CREATE POLICY "Read notifications" ON push_notifications
    FOR SELECT TO authenticated
    USING (
        recipient_role = 'all'
        OR recipient_role IN (
            SELECT role FROM users WHERE id = auth.uid() LIMIT 1
        )
    );
