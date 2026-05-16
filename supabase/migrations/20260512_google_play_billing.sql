-- Add Google Play Billing fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_purchase_token TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS google_order_id TEXT;

-- Index for looking up users by Google purchase token (used in RTDN webhook)
CREATE INDEX IF NOT EXISTS idx_users_google_purchase_token
  ON users (google_purchase_token)
  WHERE google_purchase_token IS NOT NULL;
