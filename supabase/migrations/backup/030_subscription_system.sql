-- Migration: Subscription System
-- Adds Stripe integration, invoice tracking, and enhanced account status

-- ============================================
-- 1. Stripe Integration Fields
-- ============================================

-- Stripe customer and subscription IDs
ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS stripe_subscription_id VARCHAR(255);

-- Subscription status (active, past_due, canceled, incomplete)
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_status VARCHAR(20) DEFAULT 'active';

-- ============================================
-- 2. Account Status (extends suspension)
-- ============================================

-- Account status: active, suspended (temporary), banned (permanent)
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT 'active';
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_at TIMESTAMPTZ;
ALTER TABLE users ADD COLUMN IF NOT EXISTS banned_by_user_id UUID REFERENCES users(id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS ban_reason TEXT;

-- ============================================
-- 3. Admin Subscription Override
-- ============================================

-- Allows admins to grant tiers without payment (e.g., Pro for $0)
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_override_tier VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_override_reason TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_override_by_user_id UUID REFERENCES users(id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_override_at TIMESTAMPTZ;

-- ============================================
-- 4. Invoice Cache Table
-- ============================================

CREATE TABLE IF NOT EXISTS stripe_invoices (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  stripe_invoice_id VARCHAR(255) NOT NULL UNIQUE,
  stripe_subscription_id VARCHAR(255),
  amount_cents INTEGER NOT NULL,
  tax_cents INTEGER DEFAULT 0,
  currency VARCHAR(3) DEFAULT 'usd',
  status VARCHAR(20) NOT NULL, -- paid, open, draft, void, uncollectible
  invoice_date TIMESTAMPTZ NOT NULL,
  period_start TIMESTAMPTZ,
  period_end TIMESTAMPTZ,
  hosted_invoice_url TEXT,
  invoice_pdf_url TEXT,
  payment_method_brand VARCHAR(20), -- visa, mastercard, etc.
  payment_method_last4 VARCHAR(4),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 5. Subscription Events Audit Log
-- ============================================

CREATE TABLE IF NOT EXISTS subscription_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_type VARCHAR(50) NOT NULL, -- upgrade, downgrade, cancel, reactivate, admin_override, etc.
  old_tier VARCHAR(20),
  new_tier VARCHAR(20),
  stripe_event_id VARCHAR(255),
  admin_user_id UUID REFERENCES users(id),
  notes TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================
-- 6. Indexes
-- ============================================

CREATE INDEX IF NOT EXISTS idx_users_stripe_customer
  ON users(stripe_customer_id) WHERE stripe_customer_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_account_status
  ON users(account_status) WHERE account_status != 'active';

CREATE INDEX IF NOT EXISTS idx_stripe_invoices_user
  ON stripe_invoices(user_id);

CREATE INDEX IF NOT EXISTS idx_stripe_invoices_date
  ON stripe_invoices(user_id, invoice_date DESC);

CREATE INDEX IF NOT EXISTS idx_subscription_events_user
  ON subscription_events(user_id, created_at DESC);

-- ============================================
-- 7. Comments
-- ============================================

COMMENT ON COLUMN users.stripe_customer_id IS 'Stripe customer ID for billing';
COMMENT ON COLUMN users.stripe_subscription_id IS 'Active Stripe subscription ID';
COMMENT ON COLUMN users.subscription_status IS 'Stripe subscription status: active, past_due, canceled, incomplete';
COMMENT ON COLUMN users.account_status IS 'Account status: active, suspended (temporary), banned (permanent)';
COMMENT ON COLUMN users.subscription_override_tier IS 'Admin-set tier that overrides Stripe subscription';
COMMENT ON TABLE stripe_invoices IS 'Cached invoice data from Stripe for display in profile';
COMMENT ON TABLE subscription_events IS 'Audit log of all subscription changes';

-- ============================================
-- 8. RLS Policies
-- ============================================

-- Enable RLS on new tables
ALTER TABLE stripe_invoices ENABLE ROW LEVEL SECURITY;
ALTER TABLE subscription_events ENABLE ROW LEVEL SECURITY;

-- Invoices: Users can view their own invoices
CREATE POLICY "Users can view own invoices" ON stripe_invoices
FOR SELECT USING (
  user_id = (
    SELECT id FROM users
    WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
  )
);

-- Subscription events: Users can view their own events
CREATE POLICY "Users can view own subscription events" ON subscription_events
FOR SELECT USING (
  user_id = (
    SELECT id FROM users
    WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
  )
);
