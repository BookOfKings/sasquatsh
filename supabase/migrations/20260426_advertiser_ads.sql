-- Advertiser-submitted ads with Stripe subscription tracking
CREATE TABLE advertiser_ads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  stripe_subscription_id VARCHAR(200),
  stripe_customer_id VARCHAR(200),
  ad_tier VARCHAR(20) NOT NULL CHECK (ad_tier IN ('starter', 'standard', 'premium', 'featured')),
  status VARCHAR(20) NOT NULL DEFAULT 'pending_payment' CHECK (status IN ('pending_payment', 'pending_review', 'active', 'paused', 'expired', 'rejected')),
  -- Ad content
  title VARCHAR(100) NOT NULL,
  description TEXT NOT NULL,
  image_url VARCHAR(500),
  link_url VARCHAR(500) NOT NULL,
  -- Targeting
  target_city VARCHAR(100),
  target_state VARCHAR(2),
  -- Tracking
  impression_count INTEGER NOT NULL DEFAULT 0,
  click_count INTEGER NOT NULL DEFAULT 0,
  -- Dates
  started_at TIMESTAMPTZ,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_advertiser_ads_user_id ON advertiser_ads(user_id);
CREATE INDEX idx_advertiser_ads_status ON advertiser_ads(status);

ALTER TABLE advertiser_ads ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read active ads" ON advertiser_ads FOR SELECT USING (true);
