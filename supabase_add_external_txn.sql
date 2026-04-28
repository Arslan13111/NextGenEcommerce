-- Run this in Supabase SQL Editor to add the external_transaction_id column
ALTER TABLE payment_transactions
ADD COLUMN IF NOT EXISTS external_transaction_id TEXT;
