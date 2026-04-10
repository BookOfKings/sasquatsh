-- Self-hosted advertising system

-- Ads table
CREATE TABLE ads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,  -- Internal name for admin
  advertiser_name VARCHAR(100),  -- Company/person name (null for self-promo)
  ad_type VARCHAR(20) NOT NULL DEFAULT 'banner',  -- banner, sidebar, featured
  placement VARCHAR(50) NOT NULL DEFAULT 'general',  -- general, dashboard, events, groups

  -- Creative
  image_url TEXT,  -- Ad image (stored in Supabase storage)
  title VARCHAR(100),  -- Ad headline
  description VARCHAR(255),  -- Short description
  link_url TEXT NOT NULL,  -- Where the ad goes when clicked

  -- Targeting
  target_city VARCHAR(100),  -- Optional geo-targeting
  target_state VARCHAR(50),

  -- Scheduling
  start_date DATE,
  end_date DATE,
  is_active BOOLEAN DEFAULT true,

  -- Self-promo flag
  is_house_ad BOOLEAN DEFAULT false,  -- True for self-promotion ads

  -- Metadata
  priority INTEGER DEFAULT 0,  -- Higher = shown more often
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Track impressions (views)
CREATE TABLE ad_impressions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ad_id UUID NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,  -- Null for anonymous
  page_url TEXT,
  ip_hash VARCHAR(64),  -- Hashed IP for counting unique views
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Track clicks
CREATE TABLE ad_clicks (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ad_id UUID NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  page_url TEXT,
  ip_hash VARCHAR(64),
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_ads_active ON ads(is_active, start_date, end_date);
CREATE INDEX idx_ads_placement ON ads(placement);
CREATE INDEX idx_ad_impressions_ad_id ON ad_impressions(ad_id);
CREATE INDEX idx_ad_impressions_date ON ad_impressions(created_at);
CREATE INDEX idx_ad_clicks_ad_id ON ad_clicks(ad_id);
CREATE INDEX idx_ad_clicks_date ON ad_clicks(created_at);

-- View for ad stats
CREATE VIEW ad_stats AS
SELECT
  a.id,
  a.name,
  a.advertiser_name,
  a.is_house_ad,
  a.is_active,
  a.start_date,
  a.end_date,
  COUNT(DISTINCT i.id) as impression_count,
  COUNT(DISTINCT c.id) as click_count,
  CASE
    WHEN COUNT(DISTINCT i.id) > 0
    THEN ROUND((COUNT(DISTINCT c.id)::NUMERIC / COUNT(DISTINCT i.id)::NUMERIC) * 100, 2)
    ELSE 0
  END as ctr_percent
FROM ads a
LEFT JOIN ad_impressions i ON i.ad_id = a.id
LEFT JOIN ad_clicks c ON c.ad_id = a.id
GROUP BY a.id, a.name, a.advertiser_name, a.is_house_ad, a.is_active, a.start_date, a.end_date;

-- Insert initial self-promo ads
INSERT INTO ads (name, ad_type, placement, title, description, link_url, is_house_ad, is_active, priority) VALUES
('Upgrade to Basic', 'banner', 'general', 'Host More Games!', 'Upgrade to Basic for 5 games per event, planning tools, and no ads.', '/pricing', true, true, 10),
('Upgrade to Pro', 'banner', 'general', 'Go Pro!', 'Get 10 games per event, items lists, and priority support.', '/pricing', true, true, 10),
('Create a Group', 'banner', 'dashboard', 'Start a Gaming Group', 'Organize your crew and plan epic game nights together.', '/groups/create', true, true, 5);

COMMENT ON TABLE ads IS 'Self-hosted advertising system for displaying ads to free tier users';
COMMENT ON TABLE ad_impressions IS 'Tracks when ads are viewed';
COMMENT ON TABLE ad_clicks IS 'Tracks when ads are clicked';
