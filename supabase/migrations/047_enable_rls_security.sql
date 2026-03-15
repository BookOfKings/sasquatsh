-- Migration: 047_enable_rls_security.sql
-- Purpose: Enable RLS on all tables that were flagged by Supabase security linter
-- This is a CRITICAL security fix - these tables were publicly accessible via REST API

-- ============================================================================
-- HELPER FUNCTION: Get current user's UUID from Firebase UID
-- This is used in RLS policies to get the user's internal ID
-- ============================================================================

CREATE OR REPLACE FUNCTION get_current_user_id()
RETURNS UUID AS $$
  SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
$$ LANGUAGE sql SECURITY DEFINER STABLE;

CREATE OR REPLACE FUNCTION is_current_user_admin()
RETURNS BOOLEAN AS $$
  SELECT COALESCE(
    (SELECT is_admin FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'),
    false
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- ============================================================================
-- ADMIN TABLES - Only admins should have access
-- ============================================================================

-- admin_bugs: Only admins can read/write
ALTER TABLE admin_bugs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins can do everything with bugs"
  ON admin_bugs
  FOR ALL
  USING (is_current_user_admin())
  WITH CHECK (is_current_user_admin());

-- admin_notes: Only admins can read/write
ALTER TABLE admin_notes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins can do everything with notes"
  ON admin_notes
  FOR ALL
  USING (is_current_user_admin())
  WITH CHECK (is_current_user_admin());

-- ============================================================================
-- GROUP TABLES - Members can access their group data
-- ============================================================================

-- group_join_requests: Users can see their own requests, group admins can see all for their groups
ALTER TABLE group_join_requests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view their own join requests"
  ON group_join_requests
  FOR SELECT
  USING (user_id = get_current_user_id());

CREATE POLICY "Group admins can view requests for their groups"
  ON group_join_requests
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM group_memberships
      WHERE group_memberships.group_id = group_join_requests.group_id
      AND group_memberships.user_id = get_current_user_id()
      AND group_memberships.role IN ('owner', 'admin')
    )
  );

CREATE POLICY "Users can create their own join requests"
  ON group_join_requests
  FOR INSERT
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Group admins can update requests for their groups"
  ON group_join_requests
  FOR UPDATE
  USING (
    EXISTS (
      SELECT 1 FROM group_memberships
      WHERE group_memberships.group_id = group_join_requests.group_id
      AND group_memberships.user_id = get_current_user_id()
      AND group_memberships.role IN ('owner', 'admin')
    )
  );

CREATE POLICY "Users can delete their own pending requests"
  ON group_join_requests
  FOR DELETE
  USING (user_id = get_current_user_id() AND status = 'pending');

-- group_invitations: Group admins can manage, invited users can view their invitations
ALTER TABLE group_invitations ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Group admins can manage invitations"
  ON group_invitations
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM group_memberships
      WHERE group_memberships.group_id = group_invitations.group_id
      AND group_memberships.user_id = get_current_user_id()
      AND group_memberships.role IN ('owner', 'admin')
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM group_memberships
      WHERE group_memberships.group_id = group_invitations.group_id
      AND group_memberships.user_id = get_current_user_id()
      AND group_memberships.role IN ('owner', 'admin')
    )
  );

CREATE POLICY "Invited users can view their invitations"
  ON group_invitations
  FOR SELECT
  USING (invited_user_id = get_current_user_id());

-- group_invitation_uses: Group admins can view
ALTER TABLE group_invitation_uses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Group admins can view invitation uses"
  ON group_invitation_uses
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM group_invitations gi
      JOIN group_memberships gm ON gm.group_id = gi.group_id
      WHERE gi.id = group_invitation_uses.invitation_id
      AND gm.user_id = get_current_user_id()
      AND gm.role IN ('owner', 'admin')
    )
  );

CREATE POLICY "Users can record their own invitation use"
  ON group_invitation_uses
  FOR INSERT
  WITH CHECK (user_id = get_current_user_id());

-- ============================================================================
-- PLANNING TABLES - Invitees and creators can access their planning sessions
-- ============================================================================

-- planning_sessions: Creators and invitees can access
ALTER TABLE planning_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Creators can manage their sessions"
  ON planning_sessions
  FOR ALL
  USING (created_by_user_id = get_current_user_id())
  WITH CHECK (created_by_user_id = get_current_user_id());

CREATE POLICY "Invitees can view sessions they're invited to"
  ON planning_sessions
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_sessions.id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Group members can view their group's sessions"
  ON planning_sessions
  FOR SELECT
  USING (
    group_id IS NOT NULL AND
    EXISTS (
      SELECT 1 FROM group_memberships
      WHERE group_memberships.group_id = planning_sessions.group_id
      AND group_memberships.user_id = get_current_user_id()
    )
  );

-- planning_dates: Same access as planning_sessions
ALTER TABLE planning_dates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Session creators can manage dates"
  ON planning_dates
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_dates.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_dates.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  );

CREATE POLICY "Invitees can view dates"
  ON planning_dates
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_dates.session_id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Group members can view dates for group sessions"
  ON planning_dates
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_sessions ps
      JOIN group_memberships gm ON gm.group_id = ps.group_id
      WHERE ps.id = planning_dates.session_id
      AND ps.group_id IS NOT NULL
      AND gm.user_id = get_current_user_id()
    )
  );

-- planning_date_votes: Users can manage their own votes, view votes in their sessions
ALTER TABLE planning_date_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own votes"
  ON planning_date_votes
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view all votes in their sessions"
  ON planning_date_votes
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees pi
      JOIN planning_dates pd ON pd.session_id = pi.session_id
      WHERE pd.id = planning_date_votes.date_id
      AND pi.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Session creators can view all votes"
  ON planning_date_votes
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_dates pd
      JOIN planning_sessions ps ON ps.id = pd.session_id
      WHERE pd.id = planning_date_votes.date_id
      AND ps.created_by_user_id = get_current_user_id()
    )
  );

-- planning_invitees: Session creators can manage, users can see if they're invited
ALTER TABLE planning_invitees ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Session creators can manage invitees"
  ON planning_invitees
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_invitees.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_invitees.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  );

CREATE POLICY "Users can view and update their own invitee record"
  ON planning_invitees
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view other invitees in same session"
  ON planning_invitees
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees pi2
      WHERE pi2.session_id = planning_invitees.session_id
      AND pi2.user_id = get_current_user_id()
    )
  );

-- planning_game_suggestions: Invitees can suggest and view, only suggester can delete their own
ALTER TABLE planning_game_suggestions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Invitees can view suggestions in their sessions"
  ON planning_game_suggestions
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_game_suggestions.session_id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Invitees can add suggestions"
  ON planning_game_suggestions
  FOR INSERT
  WITH CHECK (
    suggested_by_user_id = get_current_user_id() AND
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_game_suggestions.session_id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Users can delete their own suggestions"
  ON planning_game_suggestions
  FOR DELETE
  USING (suggested_by_user_id = get_current_user_id());

CREATE POLICY "Session creators can manage all suggestions"
  ON planning_game_suggestions
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_game_suggestions.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_game_suggestions.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  );

-- planning_game_votes: Users can manage their own votes
ALTER TABLE planning_game_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own game votes"
  ON planning_game_votes
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view all game votes in their sessions"
  ON planning_game_votes
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees pi
      JOIN planning_game_suggestions pgs ON pgs.session_id = pi.session_id
      WHERE pgs.id = planning_game_votes.suggestion_id
      AND pi.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Session creators can view all game votes"
  ON planning_game_votes
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_game_suggestions pgs
      JOIN planning_sessions ps ON ps.id = pgs.session_id
      WHERE pgs.id = planning_game_votes.suggestion_id
      AND ps.created_by_user_id = get_current_user_id()
    )
  );

-- planning_session_items: Invitees can manage items
ALTER TABLE planning_session_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Invitees can view and add items in their sessions"
  ON planning_session_items
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_session_items.session_id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Invitees can add items"
  ON planning_session_items
  FOR INSERT
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM planning_invitees
      WHERE planning_invitees.session_id = planning_session_items.session_id
      AND planning_invitees.user_id = get_current_user_id()
    )
  );

CREATE POLICY "Users can update items they claimed"
  ON planning_session_items
  FOR UPDATE
  USING (claimed_by_user_id = get_current_user_id());

CREATE POLICY "Session creators can manage all items"
  ON planning_session_items
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_session_items.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM planning_sessions
      WHERE planning_sessions.id = planning_session_items.session_id
      AND planning_sessions.created_by_user_id = get_current_user_id()
    )
  );

-- ============================================================================
-- ADS TABLES - Public read for active ads, admin write
-- ============================================================================

-- ads: Anyone can read active ads, admins can manage all
ALTER TABLE ads ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view active ads"
  ON ads
  FOR SELECT
  USING (is_active = true);

CREATE POLICY "Admins can manage all ads"
  ON ads
  FOR ALL
  USING (is_current_user_admin())
  WITH CHECK (is_current_user_admin());

-- ad_impressions: Anyone can insert (for tracking), admins can view
ALTER TABLE ad_impressions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can record impressions"
  ON ad_impressions
  FOR INSERT
  WITH CHECK (true);

CREATE POLICY "Admins can view impressions"
  ON ad_impressions
  FOR SELECT
  USING (is_current_user_admin());

-- ad_clicks: Anyone can insert (for tracking), admins can view
ALTER TABLE ad_clicks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can record clicks"
  ON ad_clicks
  FOR INSERT
  WITH CHECK (true);

CREATE POLICY "Admins can view clicks"
  ON ad_clicks
  FOR SELECT
  USING (is_current_user_admin());

-- ============================================================================
-- REFERENCE DATA - Public read access
-- ============================================================================

-- zip_codes: Public read-only (reference data)
ALTER TABLE zip_codes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read zip codes"
  ON zip_codes
  FOR SELECT
  USING (true);

CREATE POLICY "Admins can manage zip codes"
  ON zip_codes
  FOR ALL
  USING (is_current_user_admin())
  WITH CHECK (is_current_user_admin());

-- ============================================================================
-- FIX SECURITY DEFINER VIEWS
-- These views bypass RLS - recreate them as SECURITY INVOKER (default)
-- ============================================================================

-- Drop and recreate hot_locations view without SECURITY DEFINER
DROP VIEW IF EXISTS hot_locations;

CREATE VIEW hot_locations AS
SELECT
  el.id,
  el.name,
  el.city,
  el.state,
  el.venue,
  el.start_date,
  el.end_date,
  el.is_permanent,
  el.recurring_days,
  COUNT(DISTINCT e.id) as event_count,
  COUNT(DISTINCT er.user_id) as user_count
FROM event_locations el
LEFT JOIN events e ON e.event_location_id = el.id
LEFT JOIN event_registrations er ON er.event_id = e.id
WHERE el.status = 'approved'
  AND (
    el.is_permanent = true
    OR el.end_date >= CURRENT_DATE
    OR el.recurring_days IS NOT NULL
  )
GROUP BY el.id, el.name, el.city, el.state, el.venue, el.start_date, el.end_date, el.is_permanent, el.recurring_days
ORDER BY event_count DESC, user_count DESC
LIMIT 10;

-- Drop and recreate ad_stats view without SECURITY DEFINER
DROP VIEW IF EXISTS ad_stats;

CREATE VIEW ad_stats AS
SELECT
  a.id,
  a.name,
  a.advertiser_name,
  a.is_house_ad,
  a.is_active,
  a.start_date,
  a.end_date,
  COALESCE(imp.impression_count, 0) as impression_count,
  COALESCE(clk.click_count, 0) as click_count,
  CASE
    WHEN COALESCE(imp.impression_count, 0) > 0
    THEN ROUND((COALESCE(clk.click_count, 0)::numeric / imp.impression_count) * 100, 2)
    ELSE 0
  END as ctr_percent
FROM ads a
LEFT JOIN (
  SELECT ad_id, COUNT(*) as impression_count
  FROM ad_impressions
  GROUP BY ad_id
) imp ON imp.ad_id = a.id
LEFT JOIN (
  SELECT ad_id, COUNT(*) as click_count
  FROM ad_clicks
  GROUP BY ad_id
) clk ON clk.ad_id = a.id;

-- Grant access to authenticated users for these views
GRANT SELECT ON hot_locations TO authenticated;
GRANT SELECT ON hot_locations TO anon;
GRANT SELECT ON ad_stats TO authenticated;

-- ============================================================================
-- IMPORTANT NOTES:
-- 1. Service role key bypasses RLS - Edge functions will still work normally
-- 2. The helper functions use SECURITY DEFINER to access user data
-- 3. Anonymous users can still view active ads and record impressions/clicks
-- ============================================================================
