-- Tier 1 advisor fixes:
--   (a) Add indexes for 49 unindexed foreign keys (lint 0001) - purely additive.
--   (b) Lock down award_raffle_entry() - a SECURITY DEFINER function that awards
--       raffle entries and was callable by anon/authenticated via /rest/v1/rpc.
--       It is NOT referenced by any RLS policy, so revoking EXECUTE is safe; it is
--       only invoked from trusted server-side contexts (service role / triggers).
--
-- NOTE: The other flagged SECURITY DEFINER functions (get_current_user_id,
-- can_access_chat, is_user_session_creator, etc.) are intentionally left as-is:
-- they are used inside RLS policies that the anon role evaluates directly
-- (chat realtime, event/group reads), so revoking EXECUTE would break those reads.

-- ============================================================================
-- (a) Foreign key indexes
-- ============================================================================

CREATE INDEX IF NOT EXISTS idx_ad_clicks_user_id ON public.ad_clicks (user_id);
CREATE INDEX IF NOT EXISTS idx_ad_impressions_user_id ON public.ad_impressions (user_id);
CREATE INDEX IF NOT EXISTS idx_admin_bugs_assigned_to_user_id ON public.admin_bugs (assigned_to_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_bugs_reported_by_user_id ON public.admin_bugs (reported_by_user_id);
CREATE INDEX IF NOT EXISTS idx_admin_notes_created_by_user_id ON public.admin_notes (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_chat_moderation_actions_issued_by ON public.chat_moderation_actions (issued_by);
CREATE INDEX IF NOT EXISTS idx_chat_moderation_actions_report_id ON public.chat_moderation_actions (report_id);
CREATE INDEX IF NOT EXISTS idx_chat_reports_reporter_id ON public.chat_reports (reporter_id);
CREATE INDEX IF NOT EXISTS idx_chat_reports_reviewed_by ON public.chat_reports (reviewed_by);
CREATE INDEX IF NOT EXISTS idx_contact_submissions_replied_by ON public.contact_submissions (replied_by);
CREATE INDEX IF NOT EXISTS idx_contact_submissions_user_id ON public.contact_submissions (user_id);
CREATE INDEX IF NOT EXISTS idx_event_game_suggestions_suggested_by_user_id ON public.event_game_suggestions (suggested_by_user_id);
CREATE INDEX IF NOT EXISTS idx_event_game_votes_user_id ON public.event_game_votes (user_id);
CREATE INDEX IF NOT EXISTS idx_event_games_added_by_user_id ON public.event_games (added_by_user_id);
CREATE INDEX IF NOT EXISTS idx_event_items_claimed_by_user_id ON public.event_items (claimed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_event_items_event_id ON public.event_items (event_id);
CREATE INDEX IF NOT EXISTS idx_event_locations_approved_by_user_id ON public.event_locations (approved_by_user_id);
CREATE INDEX IF NOT EXISTS idx_event_locations_created_by_user_id ON public.event_locations (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_event_registrations_user_id ON public.event_registrations (user_id);
CREATE INDEX IF NOT EXISTS idx_events_from_planning_session_id ON public.events (from_planning_session_id);
CREATE INDEX IF NOT EXISTS idx_events_host_user_id ON public.events (host_user_id);
CREATE INDEX IF NOT EXISTS idx_first_player_statements_created_by ON public.first_player_statements (created_by);
CREATE INDEX IF NOT EXISTS idx_game_invitations_accepted_by_user_id ON public.game_invitations (accepted_by_user_id);
CREATE INDEX IF NOT EXISTS idx_game_invitations_invited_by_user_id ON public.game_invitations (invited_by_user_id);
CREATE INDEX IF NOT EXISTS idx_group_invitations_invited_by_user_id ON public.group_invitations (invited_by_user_id);
CREATE INDEX IF NOT EXISTS idx_group_join_requests_reviewed_by_user_id ON public.group_join_requests (reviewed_by_user_id);
CREATE INDEX IF NOT EXISTS idx_groups_created_by_user_id ON public.groups (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_planning_date_votes_user_id ON public.planning_date_votes (user_id);
CREATE INDEX IF NOT EXISTS idx_planning_game_suggestions_bgg_id ON public.planning_game_suggestions (bgg_id);
CREATE INDEX IF NOT EXISTS idx_planning_game_suggestions_suggested_by_user_id ON public.planning_game_suggestions (suggested_by_user_id);
CREATE INDEX IF NOT EXISTS idx_planning_game_votes_user_id ON public.planning_game_votes (user_id);
CREATE INDEX IF NOT EXISTS idx_planning_sessions_finalized_game_id ON public.planning_sessions (finalized_game_id);
CREATE INDEX IF NOT EXISTS idx_planning_sessions_created_event_id ON public.planning_sessions (created_event_id);
CREATE INDEX IF NOT EXISTS idx_planning_sessions_event_location_id ON public.planning_sessions (event_location_id);
CREATE INDEX IF NOT EXISTS idx_raffles_created_by_user_id ON public.raffles (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_raffles_winner_user_id ON public.raffles (winner_user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_games_created_by_user_id ON public.recurring_games (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_recurring_games_event_location_id ON public.recurring_games (event_location_id);
CREATE INDEX IF NOT EXISTS idx_recurring_games_host_user_id ON public.recurring_games (host_user_id);
CREATE INDEX IF NOT EXISTS idx_shareable_invite_link_uses_user_id ON public.shareable_invite_link_uses (user_id);
CREATE INDEX IF NOT EXISTS idx_shareable_invite_links_created_by_user_id ON public.shareable_invite_links (created_by_user_id);
CREATE INDEX IF NOT EXISTS idx_shareable_invite_links_event_id ON public.shareable_invite_links (event_id);
CREATE INDEX IF NOT EXISTS idx_shareable_invite_links_planning_session_id ON public.shareable_invite_links (planning_session_id);
CREATE INDEX IF NOT EXISTS idx_shareable_invite_links_recurring_game_id ON public.shareable_invite_links (recurring_game_id);
CREATE INDEX IF NOT EXISTS idx_subscription_events_admin_user_id ON public.subscription_events (admin_user_id);
CREATE INDEX IF NOT EXISTS idx_users_banned_by_user_id ON public.users (banned_by_user_id);
CREATE INDEX IF NOT EXISTS idx_users_referred_by_user_id ON public.users (referred_by_user_id);
CREATE INDEX IF NOT EXISTS idx_users_subscription_override_by_user_id ON public.users (subscription_override_by_user_id);
CREATE INDEX IF NOT EXISTS idx_users_suspended_by_user_id ON public.users (suspended_by_user_id);

-- ============================================================================
-- (b) Secure award_raffle_entry against direct RPC access
-- ============================================================================

REVOKE EXECUTE ON FUNCTION public.award_raffle_entry(uuid, character varying, uuid) FROM anon, authenticated, public;
