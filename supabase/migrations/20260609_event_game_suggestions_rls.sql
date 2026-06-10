-- Enable RLS on event game suggestion + voting tables
-- Flagged by Supabase security linter: tables were public (exposed via PostREST)
-- but RLS was not enabled.
--
-- NOTE: All application access flows through Edge Functions using the service
-- role key, which BYPASSES RLS, so enabling RLS does not affect the app. These
-- policies provide defense-in-depth and mirror the planning_game_suggestions /
-- planning_game_votes model (host = events.host_user_id, attendees =
-- event_registrations). Uses get_current_user_id() defined in 047_enable_rls_security.sql.

-- ============================================================================
-- event_game_suggestions: hosts + attendees can view, attendees can suggest
-- ============================================================================

ALTER TABLE event_game_suggestions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Hosts and attendees can view suggestions"
  ON event_game_suggestions
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM events
      WHERE events.id = event_game_suggestions.event_id
      AND events.host_user_id = get_current_user_id()
    )
    OR EXISTS (
      SELECT 1 FROM event_registrations
      WHERE event_registrations.event_id = event_game_suggestions.event_id
      AND event_registrations.user_id = get_current_user_id()
      AND event_registrations.status NOT IN ('declined', 'cancelled')
    )
  );

CREATE POLICY "Attendees can add suggestions"
  ON event_game_suggestions
  FOR INSERT
  WITH CHECK (
    suggested_by_user_id = get_current_user_id()
    AND (
      EXISTS (
        SELECT 1 FROM events
        WHERE events.id = event_game_suggestions.event_id
        AND events.host_user_id = get_current_user_id()
      )
      OR EXISTS (
        SELECT 1 FROM event_registrations
        WHERE event_registrations.event_id = event_game_suggestions.event_id
        AND event_registrations.user_id = get_current_user_id()
        AND event_registrations.status NOT IN ('declined', 'cancelled')
      )
    )
  );

CREATE POLICY "Users can delete their own suggestions"
  ON event_game_suggestions
  FOR DELETE
  USING (suggested_by_user_id = get_current_user_id());

CREATE POLICY "Hosts can manage all suggestions"
  ON event_game_suggestions
  FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM events
      WHERE events.id = event_game_suggestions.event_id
      AND events.host_user_id = get_current_user_id()
    )
  )
  WITH CHECK (
    EXISTS (
      SELECT 1 FROM events
      WHERE events.id = event_game_suggestions.event_id
      AND events.host_user_id = get_current_user_id()
    )
  );

-- ============================================================================
-- event_game_votes: users manage their own votes, hosts + attendees can view
-- ============================================================================

ALTER TABLE event_game_votes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage their own votes"
  ON event_game_votes
  FOR ALL
  USING (user_id = get_current_user_id())
  WITH CHECK (user_id = get_current_user_id());

CREATE POLICY "Hosts and attendees can view votes"
  ON event_game_votes
  FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM event_game_suggestions egs
      JOIN events e ON e.id = egs.event_id
      WHERE egs.id = event_game_votes.suggestion_id
      AND e.host_user_id = get_current_user_id()
    )
    OR EXISTS (
      SELECT 1 FROM event_game_suggestions egs
      JOIN event_registrations er ON er.event_id = egs.event_id
      WHERE egs.id = event_game_votes.suggestion_id
      AND er.user_id = get_current_user_id()
      AND er.status NOT IN ('declined', 'cancelled')
    )
  );
