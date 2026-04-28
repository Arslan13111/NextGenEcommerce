-- ============================================================
-- Reviews Table Setup — NextGenEcommerce
-- Run this in Supabase Dashboard → SQL Editor → New Query
-- ============================================================

CREATE TABLE IF NOT EXISTS reviews (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_name   TEXT NOT NULL DEFAULT '',
    rating      INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT NOT NULL DEFAULT '',
    created_at  BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW())::BIGINT * 1000
);

-- One review per user per product
CREATE UNIQUE INDEX IF NOT EXISTS reviews_user_product_idx
    ON reviews (user_id, product_id);

CREATE INDEX IF NOT EXISTS reviews_product_idx ON reviews (product_id);
CREATE INDEX IF NOT EXISTS reviews_created_at_idx ON reviews (created_at DESC);

-- ── RLS ──────────────────────────────────────────────────────
ALTER TABLE reviews ENABLE ROW LEVEL SECURITY;

-- Anyone can read reviews
CREATE POLICY "Reviews are public"
ON reviews FOR SELECT TO public USING (true);

-- Authenticated users can insert their own review
CREATE POLICY "Users can insert their own review"
ON reviews FOR INSERT TO authenticated
WITH CHECK (auth.uid() = user_id);

-- Users can update their own review
CREATE POLICY "Users can update their own review"
ON reviews FOR UPDATE TO authenticated
USING (auth.uid() = user_id);

-- Users can delete their own review
CREATE POLICY "Users can delete their own review"
ON reviews FOR DELETE TO authenticated
USING (auth.uid() = user_id);

-- ── Verify ───────────────────────────────────────────────────
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'reviews'
ORDER BY ordinal_position;
