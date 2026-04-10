-- Migration: Raffle System
-- Monthly raffle for users with entries earned through hosting, planning, and attending games

-- ============================================
-- 1. Raffles Table (Admin-managed raffles)
-- ============================================

CREATE TABLE IF NOT EXISTS raffles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  description TEXT,

  -- Prize information
  prize_name VARCHAR(200) NOT NULL,
  prize_description TEXT,
  prize_image_url TEXT,
  prize_bgg_id INTEGER, -- Link to BGG game if applicable
  prize_value_cents INTEGER, -- For legal disclosure

  -- Timing
  start_date TIMESTAMPTZ NOT NULL,
  end_date TIMESTAMPTZ NOT NULL,

  -- Legal
  terms_conditions TEXT,
  mail_in_instructions TEXT, -- "No purchase necessary" alternative entry

  -- Status
  status VARCHAR(20) NOT NULL DEFAULT 'draft', -- draft, active, ended, cancelled

  -- Winner (set after raffle ends)
  winner_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  winner_selected_at TIMESTAMPTZ,
  winner_notified_at TIMESTAMPTZ,
  winner_claimed_at TIMESTAMPTZ,

  -- Artwork/display
  banner_image_url TEXT,

  -- Audit
  created_by_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT valid_dates CHECK (end_date > start_date),
  CONSTRAINT valid_status CHECK (status IN ('draft', 'active', 'ended', 'cancelled'))
);

-- ============================================
-- 2. Raffle Entries Table
-- ============================================

CREATE TABLE IF NOT EXISTS raffle_entries (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  raffle_id UUID NOT NULL REFERENCES raffles(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  -- Entry source
  entry_type VARCHAR(30) NOT NULL, -- host_event, plan_session, attend_event, mail_in
  source_id UUID, -- event_id or planning_session_id (NULL for mail_in)

  -- Entry count (1 for free users, 2 for paid on host/plan)
  entry_count INTEGER NOT NULL DEFAULT 1,

  -- Mail-in specific
  mail_in_name VARCHAR(200),
  mail_in_address TEXT,
  mail_in_verified BOOLEAN DEFAULT FALSE,

  created_at TIMESTAMPTZ DEFAULT NOW(),

  CONSTRAINT valid_entry_type CHECK (entry_type IN ('host_event', 'plan_session', 'attend_event', 'mail_in')),
  CONSTRAINT positive_entries CHECK (entry_count > 0)
);

-- ============================================
-- 3. Indexes
-- ============================================

-- Raffle lookups
CREATE INDEX idx_raffles_status ON raffles(status);
CREATE INDEX idx_raffles_dates ON raffles(start_date, end_date);
CREATE INDEX idx_raffles_active ON raffles(status, start_date, end_date)
  WHERE status = 'active';

-- Entry lookups
CREATE INDEX idx_raffle_entries_raffle ON raffle_entries(raffle_id);
CREATE INDEX idx_raffle_entries_user ON raffle_entries(user_id);
CREATE INDEX idx_raffle_entries_source ON raffle_entries(entry_type, source_id)
  WHERE source_id IS NOT NULL;

-- Prevent duplicate entries from same source
CREATE UNIQUE INDEX idx_raffle_entries_unique_source
  ON raffle_entries(raffle_id, user_id, entry_type, source_id)
  WHERE source_id IS NOT NULL;

-- ============================================
-- 4. Helper Functions
-- ============================================

-- Get the currently active raffle (or NULL if none)
CREATE OR REPLACE FUNCTION get_active_raffle()
RETURNS UUID AS $$
  SELECT id FROM raffles
  WHERE status = 'active'
    AND start_date <= NOW()
    AND end_date > NOW()
  ORDER BY start_date DESC
  LIMIT 1;
$$ LANGUAGE sql STABLE;

-- Get entry multiplier based on user tier
CREATE OR REPLACE FUNCTION get_raffle_entry_multiplier(user_uuid UUID, entry_type VARCHAR)
RETURNS INTEGER AS $$
DECLARE
  user_tier VARCHAR;
BEGIN
  -- Founding members don't participate in raffles (they get free stuff already)
  IF EXISTS (SELECT 1 FROM users WHERE id = user_uuid AND is_founding_member = TRUE) THEN
    RETURN 0;
  END IF;

  -- Get user's subscription tier
  SELECT COALESCE(subscription_override_tier, subscription_tier, 'free')
  INTO user_tier
  FROM users
  WHERE id = user_uuid;

  -- Attending always gives 1 entry
  IF entry_type = 'attend_event' THEN
    RETURN 1;
  END IF;

  -- Hosting/planning: paid tiers get 2 entries, free gets 1
  IF user_tier IN ('basic', 'pro', 'premium') THEN
    RETURN 2;
  ELSE
    RETURN 1;
  END IF;
END;
$$ LANGUAGE plpgsql STABLE;

-- Award raffle entry (called from triggers or edge functions)
CREATE OR REPLACE FUNCTION award_raffle_entry(
  p_user_id UUID,
  p_entry_type VARCHAR,
  p_source_id UUID DEFAULT NULL
)
RETURNS UUID AS $$
DECLARE
  v_raffle_id UUID;
  v_entry_count INTEGER;
  v_entry_id UUID;
BEGIN
  -- Get active raffle
  v_raffle_id := get_active_raffle();
  IF v_raffle_id IS NULL THEN
    RETURN NULL; -- No active raffle
  END IF;

  -- Get entry multiplier
  v_entry_count := get_raffle_entry_multiplier(p_user_id, p_entry_type);
  IF v_entry_count = 0 THEN
    RETURN NULL; -- User doesn't qualify (e.g., founding member)
  END IF;

  -- Check for duplicate entry from same source
  IF p_source_id IS NOT NULL THEN
    IF EXISTS (
      SELECT 1 FROM raffle_entries
      WHERE raffle_id = v_raffle_id
        AND user_id = p_user_id
        AND entry_type = p_entry_type
        AND source_id = p_source_id
    ) THEN
      RETURN NULL; -- Already has entry from this source
    END IF;
  END IF;

  -- Insert entry
  INSERT INTO raffle_entries (raffle_id, user_id, entry_type, source_id, entry_count)
  VALUES (v_raffle_id, p_user_id, p_entry_type, p_source_id, v_entry_count)
  RETURNING id INTO v_entry_id;

  RETURN v_entry_id;
END;
$$ LANGUAGE plpgsql;

-- Select random winner (weighted by entry_count)
CREATE OR REPLACE FUNCTION select_raffle_winner(p_raffle_id UUID)
RETURNS UUID AS $$
DECLARE
  v_winner_user_id UUID;
BEGIN
  -- Verify raffle exists and is ended
  IF NOT EXISTS (
    SELECT 1 FROM raffles
    WHERE id = p_raffle_id
      AND status = 'active'
      AND end_date <= NOW()
  ) THEN
    RAISE EXCEPTION 'Raffle is not ready for winner selection';
  END IF;

  -- Select winner weighted by entry_count
  WITH weighted_entries AS (
    SELECT user_id,
           SUM(entry_count) as total_entries,
           random() as rand
    FROM raffle_entries
    WHERE raffle_id = p_raffle_id
    GROUP BY user_id
  ),
  cumulative AS (
    SELECT user_id,
           SUM(total_entries) OVER (ORDER BY rand) as cumsum,
           SUM(total_entries) OVER () as total
    FROM weighted_entries
  )
  SELECT user_id INTO v_winner_user_id
  FROM cumulative
  WHERE cumsum >= random() * total
  ORDER BY cumsum
  LIMIT 1;

  -- Update raffle with winner
  UPDATE raffles
  SET winner_user_id = v_winner_user_id,
      winner_selected_at = NOW(),
      status = 'ended'
  WHERE id = p_raffle_id;

  RETURN v_winner_user_id;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 5. RLS Policies
-- ============================================

ALTER TABLE raffles ENABLE ROW LEVEL SECURITY;
ALTER TABLE raffle_entries ENABLE ROW LEVEL SECURITY;

-- Raffles: Everyone can view active/ended raffles
CREATE POLICY "raffles_select_public" ON raffles FOR SELECT
USING (status IN ('active', 'ended'));

-- Raffles: Admins can do everything
CREATE POLICY "raffles_admin_all" ON raffles FOR ALL
USING (
  EXISTS (
    SELECT 1 FROM users
    WHERE id = get_current_user_id()
      AND is_admin = TRUE
  )
);

-- Entries: Users can view their own entries
CREATE POLICY "raffle_entries_select_own" ON raffle_entries FOR SELECT
USING (user_id = get_current_user_id());

-- Entries: Admins can view all entries
CREATE POLICY "raffle_entries_admin_select" ON raffle_entries FOR SELECT
USING (
  EXISTS (
    SELECT 1 FROM users
    WHERE id = get_current_user_id()
      AND is_admin = TRUE
  )
);

-- Entries: Insert via function only (no direct insert by users)
-- Note: The award_raffle_entry function runs with SECURITY DEFINER in edge function context

-- ============================================
-- 6. Grants
-- ============================================

GRANT SELECT ON raffles TO authenticated;
GRANT SELECT ON raffle_entries TO authenticated;

-- ============================================
-- 7. Comments
-- ============================================

COMMENT ON TABLE raffles IS 'Monthly raffles with prizes for active users';
COMMENT ON TABLE raffle_entries IS 'User entries in raffles earned through participation';
COMMENT ON COLUMN raffles.mail_in_instructions IS 'Instructions for "No purchase necessary" mail-in entry (legal compliance)';
COMMENT ON COLUMN raffle_entries.entry_type IS 'How entry was earned: host_event, plan_session, attend_event, mail_in';
COMMENT ON COLUMN raffle_entries.entry_count IS 'Number of entries (1 for free, 2 for paid on host/plan)';
COMMENT ON FUNCTION award_raffle_entry IS 'Awards raffle entry to user, returns entry ID or NULL if ineligible';
