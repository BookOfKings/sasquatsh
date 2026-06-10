-- Migration: Fix auth_rls_initplan performance lint
--
-- Supabase's `auth_rls_initplan` advisor flags RLS policies that call
-- auth.uid() or current_setting('request.jwt.claims', ...) directly in a
-- USING/WITH CHECK expression. Such calls are re-evaluated once PER ROW.
-- Wrapping each call in a scalar subselect ( SELECT ... ) makes Postgres
-- treat it as an InitPlan and evaluate it ONCE per query. The result is
-- semantically identical; only the per-row evaluation cost is removed.
--
-- Replacements applied (verbatim, to both USING and WITH CHECK):
--   auth.uid()                                       -> ( SELECT auth.uid() )
--   current_setting('request.jwt.claims'::text,true)-> ( SELECT current_setting(...) )
--
-- Policies using only private.* helper functions are NOT flagged and are
-- intentionally left untouched. 57 policies are altered below.

-- ad_clicks
ALTER POLICY "Authenticated users can record clicks" ON public.ad_clicks WITH CHECK ((( SELECT auth.uid() ) IS NOT NULL));

-- ad_impressions
ALTER POLICY "Authenticated users can record impressions" ON public.ad_impressions WITH CHECK ((( SELECT auth.uid() ) IS NOT NULL));

-- contact_submissions
ALTER POLICY "contact_submissions_select_admin" ON public.contact_submissions USING ((EXISTS ( SELECT 1
   FROM users
  WHERE ((users.id = ( SELECT auth.uid() )) AND (users.is_admin = true)))));
ALTER POLICY "contact_submissions_update_admin" ON public.contact_submissions USING ((EXISTS ( SELECT 1
   FROM users
  WHERE ((users.id = ( SELECT auth.uid() )) AND (users.is_admin = true)))));

-- event_game_sessions
ALTER POLICY "event_game_sessions_delete" ON public.event_game_sessions USING ((EXISTS ( SELECT 1
   FROM events
  WHERE ((events.id = event_game_sessions.event_id) AND (events.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "event_game_sessions_insert" ON public.event_game_sessions WITH CHECK ((EXISTS ( SELECT 1
   FROM events
  WHERE ((events.id = event_game_sessions.event_id) AND (events.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "event_game_sessions_update" ON public.event_game_sessions USING ((EXISTS ( SELECT 1
   FROM events
  WHERE ((events.id = event_game_sessions.event_id) AND (events.host_user_id = ( SELECT auth.uid() ))))));

-- event_games
ALTER POLICY "Event games readable for registered users" ON public.event_games USING ((EXISTS ( SELECT 1
   FROM (event_registrations er
     JOIN users u ON ((er.user_id = u.id)))
  WHERE ((er.event_id = event_games.event_id) AND ((u.firebase_uid)::text = (( SELECT auth.uid() ))::text)))));

-- event_locations
ALTER POLICY "Admins can delete locations" ON public.event_locations USING ((EXISTS ( SELECT 1
   FROM users
  WHERE (((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)) AND (users.is_admin = true)))));
ALTER POLICY "Authenticated users can create locations" ON public.event_locations WITH CHECK ((created_by_user_id IN ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))));
ALTER POLICY "Admins can view all locations" ON public.event_locations USING ((EXISTS ( SELECT 1
   FROM users
  WHERE (((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)) AND (users.is_admin = true)))));
ALTER POLICY "Users can view own location submissions" ON public.event_locations USING ((created_by_user_id IN ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))));
ALTER POLICY "Admins can update locations" ON public.event_locations USING ((EXISTS ( SELECT 1
   FROM users
  WHERE (((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)) AND (users.is_admin = true)))));

-- event_tables
ALTER POLICY "event_tables_delete" ON public.event_tables USING ((EXISTS ( SELECT 1
   FROM events
  WHERE ((events.id = event_tables.event_id) AND (events.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "event_tables_insert" ON public.event_tables WITH CHECK ((EXISTS ( SELECT 1
   FROM events
  WHERE ((events.id = event_tables.event_id) AND (events.host_user_id = ( SELECT auth.uid() ))))));

-- game_invitations
ALTER POLICY "Hosts can manage invitations" ON public.game_invitations USING (((invited_by_user_id = ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))) OR (event_id IN ( SELECT events.id
   FROM events
  WHERE (events.host_user_id = ( SELECT users.id
           FROM users
          WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text))))))));

-- game_session_registrations
ALTER POLICY "game_session_registrations_delete" ON public.game_session_registrations USING (((user_id = ( SELECT auth.uid() )) OR (EXISTS ( SELECT 1
   FROM (event_game_sessions egs
     JOIN events e ON ((e.id = egs.event_id)))
  WHERE ((egs.id = game_session_registrations.session_id) AND (e.host_user_id = ( SELECT auth.uid() )))))));
ALTER POLICY "game_session_registrations_insert" ON public.game_session_registrations WITH CHECK (((user_id = ( SELECT auth.uid() )) OR (EXISTS ( SELECT 1
   FROM (event_game_sessions egs
     JOIN events e ON ((e.id = egs.event_id)))
  WHERE ((egs.id = game_session_registrations.session_id) AND (e.host_user_id = ( SELECT auth.uid() )))))));

-- mtg_deck_cards
ALTER POLICY "mtg_deck_cards_owner_all" ON public.mtg_deck_cards USING ((EXISTS ( SELECT 1
   FROM mtg_decks d
  WHERE ((d.id = mtg_deck_cards.deck_id) AND (d.owner_user_id = ( SELECT auth.uid() ))))));

-- mtg_decks
ALTER POLICY "mtg_decks_owner_all" ON public.mtg_decks USING ((owner_user_id = ( SELECT auth.uid() )));

-- mtg_event_config
ALTER POLICY "mtg_event_config_manage_group_admin" ON public.mtg_event_config USING ((EXISTS ( SELECT 1
   FROM (events e
     JOIN group_memberships gm ON ((gm.group_id = e.group_id)))
  WHERE ((e.id = mtg_event_config.event_id) AND (gm.user_id = ( SELECT auth.uid() )) AND ((gm.role)::text = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::text[]))))));
ALTER POLICY "mtg_event_config_delete_host" ON public.mtg_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = mtg_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_config_insert_host" ON public.mtg_event_config WITH CHECK ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = mtg_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_config_select_host" ON public.mtg_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = mtg_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_config_update_host" ON public.mtg_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = mtg_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));

-- mtg_event_registrations
ALTER POLICY "mtg_event_reg_insert_own" ON public.mtg_event_registrations WITH CHECK ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = mtg_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_reg_select_host" ON public.mtg_event_registrations USING ((EXISTS ( SELECT 1
   FROM (event_registrations er
     JOIN events e ON ((e.id = er.event_id)))
  WHERE ((er.id = mtg_event_registrations.registration_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_reg_select_own" ON public.mtg_event_registrations USING ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = mtg_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "mtg_event_reg_update_own" ON public.mtg_event_registrations USING ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = mtg_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));

-- player_requests
ALTER POLICY "Users can manage own requests" ON public.player_requests USING ((user_id = ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))));

-- pokemon_deck_cards
ALTER POLICY "pokemon_deck_cards_owner_all" ON public.pokemon_deck_cards USING ((EXISTS ( SELECT 1
   FROM pokemon_decks d
  WHERE ((d.id = pokemon_deck_cards.deck_id) AND (d.owner_user_id = ( SELECT auth.uid() ))))));

-- pokemon_decks
ALTER POLICY "pokemon_decks_owner_all" ON public.pokemon_decks USING ((owner_user_id = ( SELECT auth.uid() )));

-- pokemon_event_config
ALTER POLICY "pokemon_event_config_manage_group_admin" ON public.pokemon_event_config USING ((EXISTS ( SELECT 1
   FROM (events e
     JOIN group_memberships gm ON ((gm.group_id = e.group_id)))
  WHERE ((e.id = pokemon_event_config.event_id) AND (gm.user_id = ( SELECT auth.uid() )) AND ((gm.role)::text = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::text[]))))));
ALTER POLICY "pokemon_event_config_delete_host" ON public.pokemon_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = pokemon_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_config_insert_host" ON public.pokemon_event_config WITH CHECK ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = pokemon_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_config_select_host" ON public.pokemon_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = pokemon_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_config_update_host" ON public.pokemon_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = pokemon_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));

-- pokemon_event_registrations
ALTER POLICY "pokemon_event_reg_insert_own" ON public.pokemon_event_registrations WITH CHECK ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = pokemon_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_reg_select_host" ON public.pokemon_event_registrations USING ((EXISTS ( SELECT 1
   FROM (event_registrations er
     JOIN events e ON ((e.id = er.event_id)))
  WHERE ((er.id = pokemon_event_registrations.registration_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_reg_select_own" ON public.pokemon_event_registrations USING ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = pokemon_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "pokemon_event_reg_update_own" ON public.pokemon_event_registrations USING ((EXISTS ( SELECT 1
   FROM event_registrations er
  WHERE ((er.id = pokemon_event_registrations.registration_id) AND (er.user_id = ( SELECT auth.uid() ))))));

-- referral_rewards
ALTER POLICY "Users can view their own rewards" ON public.referral_rewards USING ((user_id = ( SELECT auth.uid() )));

-- referrals
ALTER POLICY "Users can view their own referrals" ON public.referrals USING (((referrer_user_id = ( SELECT auth.uid() )) OR (referred_user_id = ( SELECT auth.uid() ))));

-- stripe_invoices
ALTER POLICY "Users can view own invoices" ON public.stripe_invoices USING ((user_id = ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))));

-- subscription_events
ALTER POLICY "Users can view own subscription events" ON public.subscription_events USING ((user_id = ( SELECT users.id
   FROM users
  WHERE ((users.firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)))));

-- user_game_collections
ALTER POLICY "Users can read own collection" ON public.user_game_collections USING ((((( SELECT auth.uid() ))::text = (( SELECT users.firebase_uid
   FROM users
  WHERE (users.id = user_game_collections.user_id)))::text) OR true));

-- users
ALTER POLICY "Users can update own profile" ON public.users USING (((firebase_uid)::text = ((( SELECT current_setting('request.jwt.claims'::text, true) ))::json ->> 'sub'::text)));

-- warhammer40k_event_config
ALTER POLICY "warhammer40k_event_config_manage_group_admin" ON public.warhammer40k_event_config USING ((EXISTS ( SELECT 1
   FROM (events e
     JOIN group_memberships gm ON ((gm.group_id = e.group_id)))
  WHERE ((e.id = warhammer40k_event_config.event_id) AND (gm.user_id = ( SELECT auth.uid() )) AND ((gm.role)::text = ANY ((ARRAY['admin'::character varying, 'owner'::character varying])::text[]))))));
ALTER POLICY "warhammer40k_event_config_delete_host" ON public.warhammer40k_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = warhammer40k_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "warhammer40k_event_config_insert_host" ON public.warhammer40k_event_config WITH CHECK ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = warhammer40k_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "warhammer40k_event_config_select_host" ON public.warhammer40k_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = warhammer40k_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "warhammer40k_event_config_update_host" ON public.warhammer40k_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = warhammer40k_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));

-- yugioh_event_config
ALTER POLICY "yugioh_event_config_manage_group_admin" ON public.yugioh_event_config USING ((EXISTS ( SELECT 1
   FROM (events e
     JOIN group_memberships gm ON ((gm.group_id = e.group_id)))
  WHERE ((e.id = yugioh_event_config.event_id) AND (gm.user_id = ( SELECT auth.uid() )) AND ((gm.role)::text = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::text[]))))));
ALTER POLICY "yugioh_event_config_delete_host" ON public.yugioh_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = yugioh_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "yugioh_event_config_insert_host" ON public.yugioh_event_config WITH CHECK ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = yugioh_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "yugioh_event_config_select_host" ON public.yugioh_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = yugioh_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
ALTER POLICY "yugioh_event_config_update_host" ON public.yugioh_event_config USING ((EXISTS ( SELECT 1
   FROM events e
  WHERE ((e.id = yugioh_event_config.event_id) AND (e.host_user_id = ( SELECT auth.uid() ))))));
