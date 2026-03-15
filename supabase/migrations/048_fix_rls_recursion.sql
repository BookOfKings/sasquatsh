-- Migration: 048_fix_rls_recursion.sql
-- Purpose: Fix infinite recursion in RLS policies between planning_sessions and planning_invitees
-- The issue: planning_sessions checks planning_invitees, which checks planning_sessions -> recursion

-- ============================================================================
-- HELPER FUNCTIONS: Use SECURITY DEFINER to bypass RLS when checking cross-references
-- ============================================================================

-- Check if user is invited to a session (bypasses RLS on planning_invitees)
CREATE OR REPLACE FUNCTION is_user_invited_to_session(p_session_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_invitees
    WHERE session_id = p_session_id
    AND user_id = p_user_id
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Check if user created a session (bypasses RLS on planning_sessions)
CREATE OR REPLACE FUNCTION is_user_session_creator(p_session_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions
    WHERE id = p_session_id
    AND created_by_user_id = p_user_id
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Check if user is member of the session's group (bypasses RLS)
CREATE OR REPLACE FUNCTION is_user_in_session_group(p_session_id UUID, p_user_id UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions ps
    JOIN group_memberships gm ON gm.group_id = ps.group_id
    WHERE ps.id = p_session_id
    AND ps.group_id IS NOT NULL
    AND gm.user_id = p_user_id
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Get session_id from planning_dates (bypasses RLS)
CREATE OR REPLACE FUNCTION get_session_id_from_date(p_date_id UUID)
RETURNS UUID AS $$
  SELECT session_id FROM planning_dates WHERE id = p_date_id
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Get session_id from planning_game_suggestions (bypasses RLS)
CREATE OR REPLACE FUNCTION get_session_id_from_suggestion(p_suggestion_id UUID)
RETURNS UUID AS $$
  SELECT session_id FROM planning_game_suggestions WHERE id = p_suggestion_id
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- ============================================================================
-- DROP AND RECREATE PLANNING_SESSIONS POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Creators can manage their sessions" ON planning_sessions;
DROP POLICY IF EXISTS "Invitees can view sessions they're invited to" ON planning_sessions;
DROP POLICY IF EXISTS "Group members can view their group's sessions" ON planning_sessions;

CREATE POLICY "Creators can manage their sessions"
  ON planning_sessions
  FOR ALL
  USING (created_by_user_id = get_current_user_id())
  WITH CHECK (created_by_user_id = get_current_user_id());

CREATE POLICY "Invitees can view sessions they're invited to"
  ON planning_sessions
  FOR SELECT
  USING (is_user_invited_to_session(id, get_current_user_id()));

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

-- ============================================================================
-- DROP AND RECREATE PLANNING_INVITEES POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Session creators can manage invitees" ON planning_invitees;
DROP POLICY IF EXISTS "Users can view and update their own invitee record" ON planning_invitees;
DROP POLICY IF EXISTS "Invitees can view other invitees in same session" ON planning_invitees;

CREATE POLICY "Session creators can manage invitees"
  ON planning_invitees
  FOR ALL
  USING (is_user_session_creator(session_id, get_current_user_id()))
  WITH CHECK (is_user_session_creator(session_id, get_current_user_id()));

CREATE POLICY "Users can view and update their own invitee record"
  ON planning_invitees
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view other invitees in same session"
  ON planning_invitees
  FOR SELECT
  USING (is_user_invited_to_session(session_id, get_current_user_id()));

-- ============================================================================
-- DROP AND RECREATE PLANNING_DATES POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Session creators can manage dates" ON planning_dates;
DROP POLICY IF EXISTS "Invitees can view dates" ON planning_dates;
DROP POLICY IF EXISTS "Group members can view dates for group sessions" ON planning_dates;

CREATE POLICY "Session creators can manage dates"
  ON planning_dates
  FOR ALL
  USING (is_user_session_creator(session_id, get_current_user_id()))
  WITH CHECK (is_user_session_creator(session_id, get_current_user_id()));

CREATE POLICY "Invitees can view dates"
  ON planning_dates
  FOR SELECT
  USING (is_user_invited_to_session(session_id, get_current_user_id()));

CREATE POLICY "Group members can view dates for group sessions"
  ON planning_dates
  FOR SELECT
  USING (is_user_in_session_group(session_id, get_current_user_id()));

-- ============================================================================
-- DROP AND RECREATE PLANNING_DATE_VOTES POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Users can manage their own votes" ON planning_date_votes;
DROP POLICY IF EXISTS "Invitees can view all votes in their sessions" ON planning_date_votes;
DROP POLICY IF EXISTS "Session creators can view all votes" ON planning_date_votes;

CREATE POLICY "Users can manage their own votes"
  ON planning_date_votes
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view all votes in their sessions"
  ON planning_date_votes
  FOR SELECT
  USING (is_user_invited_to_session(get_session_id_from_date(date_id), get_current_user_id()));

CREATE POLICY "Session creators can view all votes"
  ON planning_date_votes
  FOR SELECT
  USING (is_user_session_creator(get_session_id_from_date(date_id), get_current_user_id()));

-- ============================================================================
-- DROP AND RECREATE PLANNING_GAME_SUGGESTIONS POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Invitees can view suggestions in their sessions" ON planning_game_suggestions;
DROP POLICY IF EXISTS "Invitees can add suggestions" ON planning_game_suggestions;
DROP POLICY IF EXISTS "Users can delete their own suggestions" ON planning_game_suggestions;
DROP POLICY IF EXISTS "Session creators can manage all suggestions" ON planning_game_suggestions;

CREATE POLICY "Invitees can view suggestions in their sessions"
  ON planning_game_suggestions
  FOR SELECT
  USING (is_user_invited_to_session(session_id, get_current_user_id()));

CREATE POLICY "Invitees can add suggestions"
  ON planning_game_suggestions
  FOR INSERT
  WITH CHECK (
    suggested_by_user_id = get_current_user_id() AND
    is_user_invited_to_session(session_id, get_current_user_id())
  );

CREATE POLICY "Users can delete their own suggestions"
  ON planning_game_suggestions
  FOR DELETE
  USING (suggested_by_user_id = get_current_user_id());

CREATE POLICY "Session creators can manage all suggestions"
  ON planning_game_suggestions
  FOR ALL
  USING (is_user_session_creator(session_id, get_current_user_id()))
  WITH CHECK (is_user_session_creator(session_id, get_current_user_id()));

-- ============================================================================
-- DROP AND RECREATE PLANNING_GAME_VOTES POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Users can manage their own game votes" ON planning_game_votes;
DROP POLICY IF EXISTS "Invitees can view all game votes in their sessions" ON planning_game_votes;
DROP POLICY IF EXISTS "Session creators can view all game votes" ON planning_game_votes;

CREATE POLICY "Users can manage their own game votes"
  ON planning_game_votes
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Invitees can view all game votes in their sessions"
  ON planning_game_votes
  FOR SELECT
  USING (is_user_invited_to_session(get_session_id_from_suggestion(suggestion_id), get_current_user_id()));

CREATE POLICY "Session creators can view all game votes"
  ON planning_game_votes
  FOR SELECT
  USING (is_user_session_creator(get_session_id_from_suggestion(suggestion_id), get_current_user_id()));

-- ============================================================================
-- DROP AND RECREATE PLANNING_SESSION_ITEMS POLICIES
-- ============================================================================

DROP POLICY IF EXISTS "Invitees can view and add items in their sessions" ON planning_session_items;
DROP POLICY IF EXISTS "Invitees can add items" ON planning_session_items;
DROP POLICY IF EXISTS "Users can update items they claimed" ON planning_session_items;
DROP POLICY IF EXISTS "Session creators can manage all items" ON planning_session_items;

CREATE POLICY "Invitees can view items in their sessions"
  ON planning_session_items
  FOR SELECT
  USING (is_user_invited_to_session(session_id, get_current_user_id()));

CREATE POLICY "Invitees can add items"
  ON planning_session_items
  FOR INSERT
  WITH CHECK (is_user_invited_to_session(session_id, get_current_user_id()));

CREATE POLICY "Users can update items they claimed"
  ON planning_session_items
  FOR UPDATE
  USING (claimed_by_user_id = get_current_user_id());

CREATE POLICY "Session creators can manage all items"
  ON planning_session_items
  FOR ALL
  USING (is_user_session_creator(session_id, get_current_user_id()))
  WITH CHECK (is_user_session_creator(session_id, get_current_user_id()));
