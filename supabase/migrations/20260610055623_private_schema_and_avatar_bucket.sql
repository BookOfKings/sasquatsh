-- Advisor fixes (chunk A+B):
--   A) Move 12 RLS-helper SECURITY DEFINER functions out of the PostgREST-exposed
--      `public` schema into a `private` schema. This clears lints 0028/0029
--      (anon/authenticated can execute SECURITY DEFINER funcs via /rest/v1/rpc)
--      WITHOUT breaking RLS: ALTER FUNCTION ... SET SCHEMA preserves the function
--      OID, and RLS policies reference functions by OID, so every existing policy
--      keeps working untouched. PostgREST does not introspect the `private` schema,
--      so these functions are no longer exposed as RPC endpoints.
--
--      NOTE: award_raffle_entry is intentionally NOT moved - edge functions invoke
--      it via supabase.rpc('award_raffle_entry') (the PostgREST RPC endpoint), so it
--      must stay in `public`. It was already secured by revoking anon/authenticated.
--
--   B) Drop the broad "Avatars are publicly viewable" SELECT policy on the public
--      `avatars` bucket (lint 0025). Object display uses public-bucket URLs (no SELECT
--      policy needed) and the profile edge function lists via the service role
--      (bypasses RLS), so removing the enumeration policy is safe.

-- ============================================================================
-- A) Private schema for RLS helper functions
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS private;

-- Caller roles need USAGE on the schema to resolve/execute the helpers inside RLS.
-- EXECUTE grants travel with the functions via SET SCHEMA. PostgREST still does not
-- expose `private`, so this does not re-create the RPC exposure.
GRANT USAGE ON SCHEMA private TO anon, authenticated, service_role;

ALTER FUNCTION public.can_access_chat(public.chat_context_type, uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.get_current_user_id() SET SCHEMA private;
ALTER FUNCTION public.get_session_id_from_date(uuid) SET SCHEMA private;
ALTER FUNCTION public.get_session_id_from_suggestion(uuid) SET SCHEMA private;
ALTER FUNCTION public.is_current_user_admin() SET SCHEMA private;
ALTER FUNCTION public.is_event_chat_participant(uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.is_group_chat_participant(uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.is_planning_chat_participant(uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.is_user_chat_muted(uuid) SET SCHEMA private;
ALTER FUNCTION public.is_user_in_session_group(uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.is_user_invited_to_session(uuid, uuid) SET SCHEMA private;
ALTER FUNCTION public.is_user_session_creator(uuid, uuid) SET SCHEMA private;

-- can_access_chat (plpgsql) calls the is_*_chat_participant helpers by unqualified
-- name; now that those live in `private`, its search_path must include `private`.
ALTER FUNCTION private.can_access_chat(public.chat_context_type, uuid, uuid)
  SET search_path TO 'private', 'public';

-- ============================================================================
-- B) Avatars bucket: remove broad listing policy
-- ============================================================================

DROP POLICY IF EXISTS "Avatars are publicly viewable" ON storage.objects;
