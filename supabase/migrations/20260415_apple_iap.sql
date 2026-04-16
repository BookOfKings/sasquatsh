-- Add Apple IAP fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS apple_original_transaction_id TEXT UNIQUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_source TEXT;

-- Add index for Apple transaction lookups (used by App Store Server Notifications)
CREATE INDEX IF NOT EXISTS idx_users_apple_transaction ON users (apple_original_transaction_id) WHERE apple_original_transaction_id IS NOT NULL;

-- Backfill subscription_source for existing Stripe subscribers
UPDATE users SET subscription_source = 'stripe' WHERE stripe_subscription_id IS NOT NULL AND subscription_source IS NULL;
