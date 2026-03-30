


SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;


CREATE SCHEMA IF NOT EXISTS "public";


ALTER SCHEMA "public" OWNER TO "pg_database_owner";

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "pg_trgm" WITH SCHEMA "public";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp" WITH SCHEMA "extensions";

COMMENT ON SCHEMA "public" IS 'standard public schema';



CREATE TYPE "public"."chat_context_type" AS ENUM (
    'event',
    'group',
    'planning'
);


ALTER TYPE "public"."chat_context_type" OWNER TO "postgres";


CREATE TYPE "public"."chat_moderation_action" AS ENUM (
    'warning',
    'mute_1h',
    'mute_24h',
    'mute_7d',
    'ban_chat'
);


ALTER TYPE "public"."chat_moderation_action" OWNER TO "postgres";


CREATE TYPE "public"."chat_report_reason" AS ENUM (
    'harassment',
    'spam',
    'hate_speech',
    'inappropriate',
    'threats',
    'other'
);


ALTER TYPE "public"."chat_report_reason" OWNER TO "postgres";


CREATE TYPE "public"."chat_report_status" AS ENUM (
    'pending',
    'reviewed',
    'action_taken',
    'dismissed'
);


ALTER TYPE "public"."chat_report_status" OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid" DEFAULT NULL::"uuid") RETURNS "uuid"
    LANGUAGE "plpgsql"
    AS $$
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
$$;


ALTER FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid") OWNER TO "postgres";


COMMENT ON FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid") IS 'Awards raffle entry to user, returns entry ID or NULL if ineligible';



CREATE OR REPLACE FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) RETURNS numeric
    LANGUAGE "plpgsql" IMMUTABLE
    AS $$
DECLARE
  R CONSTANT DECIMAL := 3959; -- Earth's radius in miles
  dlat DECIMAL;
  dlon DECIMAL;
  a DECIMAL;
  c DECIMAL;
BEGIN
  dlat := RADIANS(lat2 - lat1);
  dlon := RADIANS(lon2 - lon1);
  a := SIN(dlat/2) * SIN(dlat/2) + COS(RADIANS(lat1)) * COS(RADIANS(lat2)) * SIN(dlon/2) * SIN(dlon/2);
  c := 2 * ATAN2(SQRT(a), SQRT(1-a));
  RETURN R * c;
END;
$$;


ALTER FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) OWNER TO "postgres";


COMMENT ON FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) IS 'Calculate distance between two lat/long points using Haversine formula';



CREATE OR REPLACE FUNCTION "public"."can_access_chat"("ctx_type" "public"."chat_context_type", "ctx_id" "uuid", "user_uuid" "uuid") RETURNS boolean
    LANGUAGE "plpgsql" STABLE SECURITY DEFINER
    AS $$
BEGIN
  CASE ctx_type
    WHEN 'event' THEN
      RETURN is_event_chat_participant(ctx_id, user_uuid);
    WHEN 'group' THEN
      RETURN is_group_chat_participant(ctx_id, user_uuid);
    WHEN 'planning' THEN
      RETURN is_planning_chat_participant(ctx_id, user_uuid);
    ELSE
      RETURN FALSE;
  END CASE;
END;
$$;


ALTER FUNCTION "public"."can_access_chat"("ctx_type" "public"."chat_context_type", "ctx_id" "uuid", "user_uuid" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."clean_bgg_cache"() RETURNS "void"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
    DELETE FROM bgg_games_cache
    WHERE cached_at < NOW() - INTERVAL '7 days'
    AND bgg_id NOT IN (SELECT DISTINCT bgg_id FROM event_games WHERE bgg_id IS NOT NULL);
END;
$$;


ALTER FUNCTION "public"."clean_bgg_cache"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."generate_slug"("name" "text") RETURNS "text"
    LANGUAGE "plpgsql"
    AS $$
DECLARE
    base_slug TEXT;
    final_slug TEXT;
    counter INT := 0;
BEGIN
    -- Convert to lowercase, replace spaces with hyphens, remove special chars
    base_slug := lower(regexp_replace(name, '[^a-zA-Z0-9\s-]', '', 'g'));
    base_slug := regexp_replace(base_slug, '\s+', '-', 'g');
    base_slug := regexp_replace(base_slug, '-+', '-', 'g');
    base_slug := trim(both '-' from base_slug);

    final_slug := base_slug;

    -- Check for uniqueness and append number if needed
    WHILE EXISTS (SELECT 1 FROM groups WHERE slug = final_slug) LOOP
        counter := counter + 1;
        final_slug := base_slug || '-' || counter;
    END LOOP;

    RETURN final_slug;
END;
$$;


ALTER FUNCTION "public"."generate_slug"("name" "text") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_active_raffle"() RETURNS "uuid"
    LANGUAGE "sql" STABLE
    AS $$
  SELECT id FROM raffles
  WHERE status = 'active'
    AND start_date <= NOW()
    AND end_date > NOW()
  ORDER BY start_date DESC
  LIMIT 1;
$$;


ALTER FUNCTION "public"."get_active_raffle"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_current_user_id"() RETURNS "uuid"
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT id FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'
$$;


ALTER FUNCTION "public"."get_current_user_id"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_popular_games"("limit_count" integer DEFAULT 10) RETURNS TABLE("game_name" "text", "bgg_id" bigint, "usage_count" bigint, "thumbnail_url" "text")
    LANGUAGE "plpgsql" STABLE
    AS $$
BEGIN
  RETURN QUERY
  SELECT
    eg.game_name::TEXT,
    eg.bgg_id,
    COUNT(*)::BIGINT as usage_count,
    MAX(eg.thumbnail_url)::TEXT as thumbnail_url
  FROM event_games eg
  WHERE eg.game_name IS NOT NULL AND eg.game_name != ''
  GROUP BY eg.game_name, eg.bgg_id
  ORDER BY usage_count DESC, eg.game_name ASC
  LIMIT limit_count;
END;
$$;


ALTER FUNCTION "public"."get_popular_games"("limit_count" integer) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_raffle_entry_multiplier"("user_uuid" "uuid", "entry_type" character varying) RETURNS integer
    LANGUAGE "plpgsql"
    AS $$
DECLARE
  user_tier VARCHAR;
  user_is_test BOOLEAN;
  user_is_founding BOOLEAN;
BEGIN
  -- Get user flags
  SELECT is_founding_member, COALESCE(is_test_account, FALSE)
  INTO user_is_founding, user_is_test
  FROM users
  WHERE id = user_uuid;

  -- Founding members don't participate in raffles (they get free stuff already)
  IF user_is_founding THEN
    RETURN 0;
  END IF;

  -- Test accounts are not eligible for raffles
  IF user_is_test THEN
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
$$;


ALTER FUNCTION "public"."get_raffle_entry_multiplier"("user_uuid" "uuid", "entry_type" character varying) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_session_id_from_date"("p_date_id" "uuid") RETURNS "uuid"
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT session_id FROM planning_dates WHERE id = p_date_id
$$;


ALTER FUNCTION "public"."get_session_id_from_date"("p_date_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_session_id_from_suggestion"("p_suggestion_id" "uuid") RETURNS "uuid"
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT session_id FROM planning_game_suggestions WHERE id = p_suggestion_id
$$;


ALTER FUNCTION "public"."get_session_id_from_suggestion"("p_suggestion_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) RETURNS TABLE("zip" character varying, "distance_miles" numeric)
    LANGUAGE "plpgsql" STABLE
    AS $$
DECLARE
  center_lat DECIMAL;
  center_lon DECIMAL;
BEGIN
  -- Get center coordinates
  SELECT latitude, longitude INTO center_lat, center_lon
  FROM zip_codes WHERE zip_codes.zip = center_zip;

  IF center_lat IS NULL THEN
    RETURN;
  END IF;

  -- Return all zips within radius
  RETURN QUERY
  SELECT z.zip, calculate_distance_miles(center_lat, center_lon, z.latitude, z.longitude) as distance_miles
  FROM zip_codes z
  WHERE calculate_distance_miles(center_lat, center_lon, z.latitude, z.longitude) <= radius_miles
  ORDER BY distance_miles;
END;
$$;


ALTER FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) OWNER TO "postgres";


COMMENT ON FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) IS 'Find all zip codes within X miles of a given zip code';



CREATE OR REPLACE FUNCTION "public"."is_current_user_admin"() RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT COALESCE(
    (SELECT is_admin FROM users WHERE firebase_uid = current_setting('request.jwt.claims', true)::json->>'sub'),
    false
  )
$$;


ALTER FUNCTION "public"."is_current_user_admin"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_event_chat_participant"("event_uuid" "uuid", "user_uuid" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM events WHERE id = event_uuid AND host_user_id = user_uuid
    UNION
    SELECT 1 FROM event_registrations
    WHERE event_id = event_uuid
    AND user_id = user_uuid
    AND status IN ('pending', 'confirmed')
  )
$$;


ALTER FUNCTION "public"."is_event_chat_participant"("event_uuid" "uuid", "user_uuid" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_group_chat_participant"("group_uuid" "uuid", "user_uuid" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM group_memberships
    WHERE group_id = group_uuid AND user_id = user_uuid
  )
$$;


ALTER FUNCTION "public"."is_group_chat_participant"("group_uuid" "uuid", "user_uuid" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_planning_chat_participant"("session_uuid" "uuid", "user_uuid" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions
    WHERE id = session_uuid AND created_by_user_id = user_uuid
    UNION
    SELECT 1 FROM planning_invitees
    WHERE session_id = session_uuid AND user_id = user_uuid
  )
$$;


ALTER FUNCTION "public"."is_planning_chat_participant"("session_uuid" "uuid", "user_uuid" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_user_chat_muted"("user_uuid" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM chat_moderation_actions
    WHERE user_id = user_uuid
    AND action IN ('mute_1h', 'mute_24h', 'mute_7d', 'ban_chat')
    AND (expires_at IS NULL OR expires_at > NOW())
  )
$$;


ALTER FUNCTION "public"."is_user_chat_muted"("user_uuid" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_user_in_session_group"("p_session_id" "uuid", "p_user_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions ps
    JOIN group_memberships gm ON gm.group_id = ps.group_id
    WHERE ps.id = p_session_id
    AND ps.group_id IS NOT NULL
    AND gm.user_id = p_user_id
  )
$$;


ALTER FUNCTION "public"."is_user_in_session_group"("p_session_id" "uuid", "p_user_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_user_invited_to_session"("p_session_id" "uuid", "p_user_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_invitees
    WHERE session_id = p_session_id
    AND user_id = p_user_id
  )
$$;


ALTER FUNCTION "public"."is_user_invited_to_session"("p_session_id" "uuid", "p_user_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."is_user_session_creator"("p_session_id" "uuid", "p_user_id" "uuid") RETURNS boolean
    LANGUAGE "sql" STABLE SECURITY DEFINER
    AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions
    WHERE id = p_session_id
    AND created_by_user_id = p_user_id
  )
$$;


ALTER FUNCTION "public"."is_user_session_creator"("p_session_id" "uuid", "p_user_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."pokemon_cards_search_vector_update"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('simple', COALESCE(NEW.name, '')), 'A') ||
    setweight(to_tsvector('simple', COALESCE(NEW.set_name, '')), 'B') ||
    setweight(to_tsvector('simple', COALESCE(array_to_string(NEW.subtypes, ' '), '')), 'C');
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."pokemon_cards_search_vector_update"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."search_bgg_cache"("search_query" "text", "result_limit" integer DEFAULT 20) RETURNS TABLE("bgg_id" integer, "name" character varying, "year_published" integer, "thumbnail_url" character varying, "min_players" integer, "max_players" integer, "playing_time" integer, "bgg_rank" integer, "average_rating" numeric, "relevance" real)
    LANGUAGE "plpgsql"
    AS $$
BEGIN
    RETURN QUERY
    SELECT
        c.bgg_id,
        c.name,
        c.year_published,
        c.thumbnail_url,
        c.min_players,
        c.max_players,
        c.playing_time,
        c.bgg_rank,
        c.average_rating,
        (
            -- Combine full-text rank with name similarity
            ts_rank(c.search_vector, plainto_tsquery('english', search_query)) * 2 +
            similarity(c.name, search_query) * 3 +
            CASE WHEN c.bgg_rank IS NOT NULL THEN 1.0 / (c.bgg_rank + 1) ELSE 0 END
        )::REAL AS relevance
    FROM bgg_games_cache c
    WHERE
        c.search_vector @@ plainto_tsquery('english', search_query)
        OR c.name ILIKE '%' || search_query || '%'
        OR similarity(c.name, search_query) > 0.3
    ORDER BY relevance DESC, c.num_ratings DESC NULLS LAST
    LIMIT result_limit;
END;
$$;


ALTER FUNCTION "public"."search_bgg_cache"("search_query" "text", "result_limit" integer) OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."select_raffle_winner"("p_raffle_id" "uuid") RETURNS "uuid"
    LANGUAGE "plpgsql"
    AS $$
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
$$;


ALTER FUNCTION "public"."select_raffle_winner"("p_raffle_id" "uuid") OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_bgg_search_vector"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', COALESCE(NEW.name, '')), 'A') ||
        setweight(to_tsvector('english', COALESCE(NEW.description, '')), 'C') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.categories, ' '), '')), 'B') ||
        setweight(to_tsvector('english', COALESCE(array_to_string(NEW.mechanics, ' '), '')), 'B');
    RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_bgg_search_vector"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_deck_card_count"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
DECLARE
  deck_uuid UUID;
BEGIN
  IF TG_OP = 'DELETE' THEN
    deck_uuid := OLD.deck_id;
  ELSE
    deck_uuid := NEW.deck_id;
  END IF;

  UPDATE mtg_decks
  SET card_count = (
    SELECT COALESCE(SUM(quantity), 0)
    FROM mtg_deck_cards
    WHERE deck_id = deck_uuid
    AND board IN ('main', 'commander')
  ),
  updated_at = NOW()
  WHERE id = deck_uuid;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_deck_card_count"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_groups_updated_at"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_groups_updated_at"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_mtg_deck_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_mtg_deck_timestamp"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_mtg_event_config_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_mtg_event_config_timestamp"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_mtg_event_registration_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_mtg_event_registration_timestamp"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_pokemon_deck_counts"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
DECLARE
  deck_uuid UUID;
BEGIN
  IF TG_OP = 'DELETE' THEN
    deck_uuid := OLD.deck_id;
  ELSE
    deck_uuid := NEW.deck_id;
  END IF;

  UPDATE pokemon_decks
  SET
    card_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
    ),
    pokemon_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Pokémon'
    ),
    trainer_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Trainer'
    ),
    energy_count = (
      SELECT COALESCE(SUM(quantity), 0)
      FROM pokemon_deck_cards
      WHERE deck_id = deck_uuid
      AND supertype = 'Energy'
    ),
    updated_at = NOW()
  WHERE id = deck_uuid;

  IF TG_OP = 'DELETE' THEN
    RETURN OLD;
  END IF;
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_pokemon_deck_counts"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_pokemon_deck_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_pokemon_deck_timestamp"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_pokemon_event_config_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_pokemon_event_config_timestamp"() OWNER TO "postgres";


CREATE OR REPLACE FUNCTION "public"."update_pokemon_event_registration_timestamp"() RETURNS "trigger"
    LANGUAGE "plpgsql"
    AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;


ALTER FUNCTION "public"."update_pokemon_event_registration_timestamp"() OWNER TO "postgres";

SET default_tablespace = '';

SET default_table_access_method = "heap";


CREATE TABLE IF NOT EXISTS "public"."ad_clicks" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "ad_id" "uuid" NOT NULL,
    "user_id" "uuid",
    "page_url" "text",
    "ip_hash" character varying(64),
    "created_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."ad_clicks" OWNER TO "postgres";


COMMENT ON TABLE "public"."ad_clicks" IS 'Tracks when ads are clicked';



CREATE TABLE IF NOT EXISTS "public"."ad_impressions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "ad_id" "uuid" NOT NULL,
    "user_id" "uuid",
    "page_url" "text",
    "ip_hash" character varying(64),
    "created_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."ad_impressions" OWNER TO "postgres";


COMMENT ON TABLE "public"."ad_impressions" IS 'Tracks when ads are viewed';



CREATE TABLE IF NOT EXISTS "public"."ads" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" character varying(100) NOT NULL,
    "advertiser_name" character varying(100),
    "ad_type" character varying(20) DEFAULT 'banner'::character varying NOT NULL,
    "placement" character varying(50) DEFAULT 'general'::character varying NOT NULL,
    "image_url" "text",
    "title" character varying(100),
    "description" character varying(255),
    "link_url" "text" NOT NULL,
    "target_city" character varying(100),
    "target_state" character varying(50),
    "start_date" "date",
    "end_date" "date",
    "is_active" boolean DEFAULT true,
    "is_house_ad" boolean DEFAULT false,
    "priority" integer DEFAULT 0,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."ads" OWNER TO "postgres";


COMMENT ON TABLE "public"."ads" IS 'Self-hosted advertising system for displaying ads to free tier users';



CREATE OR REPLACE VIEW "public"."ad_stats" AS
 SELECT "a"."id",
    "a"."name",
    "a"."advertiser_name",
    "a"."is_house_ad",
    "a"."is_active",
    "a"."start_date",
    "a"."end_date",
    COALESCE("imp"."impression_count", (0)::bigint) AS "impression_count",
    COALESCE("clk"."click_count", (0)::bigint) AS "click_count",
        CASE
            WHEN (COALESCE("imp"."impression_count", (0)::bigint) > 0) THEN "round"((((COALESCE("clk"."click_count", (0)::bigint))::numeric / ("imp"."impression_count")::numeric) * (100)::numeric), 2)
            ELSE (0)::numeric
        END AS "ctr_percent"
   FROM (("public"."ads" "a"
     LEFT JOIN ( SELECT "ad_impressions"."ad_id",
            "count"(*) AS "impression_count"
           FROM "public"."ad_impressions"
          GROUP BY "ad_impressions"."ad_id") "imp" ON (("imp"."ad_id" = "a"."id")))
     LEFT JOIN ( SELECT "ad_clicks"."ad_id",
            "count"(*) AS "click_count"
           FROM "public"."ad_clicks"
          GROUP BY "ad_clicks"."ad_id") "clk" ON (("clk"."ad_id" = "a"."id")));


ALTER VIEW "public"."ad_stats" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."admin_bugs" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "title" character varying(200) NOT NULL,
    "description" "text",
    "steps_to_reproduce" "text",
    "status" character varying(20) DEFAULT 'open'::character varying,
    "priority" character varying(20) DEFAULT 'medium'::character varying,
    "reported_by_user_id" "uuid" NOT NULL,
    "assigned_to_user_id" "uuid",
    "resolved_at" timestamp with time zone,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "admin_bugs_priority_check" CHECK ((("priority")::"text" = ANY ((ARRAY['low'::character varying, 'medium'::character varying, 'high'::character varying, 'critical'::character varying])::"text"[]))),
    CONSTRAINT "admin_bugs_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['open'::character varying, 'in_progress'::character varying, 'resolved'::character varying, 'closed'::character varying, 'wont_fix'::character varying])::"text"[])))
);


ALTER TABLE "public"."admin_bugs" OWNER TO "postgres";


COMMENT ON TABLE "public"."admin_bugs" IS 'Bug tracking for the application';



CREATE TABLE IF NOT EXISTS "public"."admin_notes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "title" character varying(200) NOT NULL,
    "content" "text" NOT NULL,
    "category" character varying(50) DEFAULT 'general'::character varying,
    "is_pinned" boolean DEFAULT false,
    "created_by_user_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    "is_implemented" boolean DEFAULT false
);


ALTER TABLE "public"."admin_notes" OWNER TO "postgres";


COMMENT ON TABLE "public"."admin_notes" IS 'Project notes and documentation for admins';



CREATE TABLE IF NOT EXISTS "public"."bgg_games_cache" (
    "bgg_id" integer NOT NULL,
    "name" character varying(255) NOT NULL,
    "year_published" integer,
    "thumbnail_url" character varying(500),
    "image_url" character varying(500),
    "min_players" integer,
    "max_players" integer,
    "min_playtime" integer,
    "max_playtime" integer,
    "playing_time" integer,
    "weight" numeric(3,2),
    "description" "text",
    "categories" "text"[],
    "mechanics" "text"[],
    "cached_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "search_vector" "tsvector",
    "bgg_rank" integer,
    "num_ratings" integer DEFAULT 0,
    "average_rating" numeric(4,2)
);


ALTER TABLE "public"."bgg_games_cache" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."chat_messages" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "context_type" "public"."chat_context_type" NOT NULL,
    "context_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "content" "text" NOT NULL,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone,
    "is_deleted" boolean DEFAULT false,
    CONSTRAINT "content_max_length" CHECK (("char_length"("content") <= 1000)),
    CONSTRAINT "content_not_empty" CHECK (("char_length"(TRIM(BOTH FROM "content")) > 0))
);


ALTER TABLE "public"."chat_messages" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."chat_moderation_actions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "action" "public"."chat_moderation_action" NOT NULL,
    "reason" "text" NOT NULL,
    "report_id" "uuid",
    "issued_by" "uuid" NOT NULL,
    "expires_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."chat_moderation_actions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."chat_reports" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "message_id" "uuid" NOT NULL,
    "reporter_id" "uuid" NOT NULL,
    "reason" "public"."chat_report_reason" NOT NULL,
    "details" "text",
    "status" "public"."chat_report_status" DEFAULT 'pending'::"public"."chat_report_status" NOT NULL,
    "reviewed_by" "uuid",
    "reviewed_at" timestamp without time zone,
    "admin_notes" "text",
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."chat_reports" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."contact_submissions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" character varying(100) NOT NULL,
    "email" character varying(255) NOT NULL,
    "subject" character varying(50) NOT NULL,
    "message" "text" NOT NULL,
    "user_id" "uuid",
    "ip_address" character varying(45),
    "user_agent" "text",
    "status" character varying(20) DEFAULT 'new'::character varying,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "replied_at" timestamp with time zone,
    "replied_by" "uuid",
    CONSTRAINT "contact_submissions_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['new'::character varying, 'read'::character varying, 'replied'::character varying, 'closed'::character varying])::"text"[])))
);


ALTER TABLE "public"."contact_submissions" OWNER TO "postgres";


COMMENT ON TABLE "public"."contact_submissions" IS 'Contact form submissions from users and visitors';



CREATE TABLE IF NOT EXISTS "public"."event_game_sessions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "table_id" "uuid" NOT NULL,
    "bgg_id" integer,
    "game_name" character varying(200) NOT NULL,
    "thumbnail_url" "text",
    "min_players" integer,
    "max_players" integer,
    "slot_index" integer DEFAULT 0 NOT NULL,
    "start_time" time without time zone,
    "duration_minutes" integer DEFAULT 60 NOT NULL,
    "status" character varying(20) DEFAULT 'scheduled'::character varying,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."event_game_sessions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."event_games" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "bgg_id" integer,
    "game_name" character varying(160) NOT NULL,
    "thumbnail_url" character varying(500),
    "min_players" integer,
    "max_players" integer,
    "playing_time" integer,
    "is_primary" boolean DEFAULT false NOT NULL,
    "is_alternative" boolean DEFAULT false NOT NULL,
    "added_by_user_id" "uuid",
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."event_games" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."event_items" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "item_name" character varying(100) NOT NULL,
    "item_category" character varying(20) DEFAULT 'other'::character varying NOT NULL,
    "quantity_needed" integer DEFAULT 1 NOT NULL,
    "claimed_by_user_id" "uuid",
    "claimed_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "event_items_item_category_check" CHECK ((("item_category")::"text" = ANY ((ARRAY['food'::character varying, 'drinks'::character varying, 'supplies'::character varying, 'games'::character varying, 'other'::character varying])::"text"[])))
);


ALTER TABLE "public"."event_items" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."event_locations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" character varying(200) NOT NULL,
    "name_normalized" character varying(200) NOT NULL,
    "city" character varying(100) NOT NULL,
    "state" character varying(50) NOT NULL,
    "venue" character varying(200),
    "start_date" "date",
    "end_date" "date",
    "status" character varying(20) DEFAULT 'pending'::character varying,
    "created_by_user_id" "uuid",
    "approved_by_user_id" "uuid",
    "approved_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "is_permanent" boolean DEFAULT false NOT NULL,
    "recurring_days" integer[],
    "timezone" character varying(50),
    "postal_code" "text",
    CONSTRAINT "event_locations_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::"text"[])))
);


ALTER TABLE "public"."event_locations" OWNER TO "postgres";


COMMENT ON COLUMN "public"."event_locations"."is_permanent" IS 'Permanent locations like game stores that never expire';



COMMENT ON COLUMN "public"."event_locations"."recurring_days" IS 'Array of day numbers (0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat) when location is active';



COMMENT ON COLUMN "public"."event_locations"."timezone" IS 'Venue timezone (IANA timezone name). Used when creating events at this venue.';



CREATE TABLE IF NOT EXISTS "public"."event_registrations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "status" character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    "registered_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "event_registrations_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['pending'::character varying, 'confirmed'::character varying, 'declined'::character varying, 'cancelled'::character varying, 'waitlist'::character varying])::"text"[])))
);


ALTER TABLE "public"."event_registrations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."event_tables" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "table_number" integer NOT NULL,
    "table_name" character varying(50),
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."event_tables" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."events" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "host_user_id" "uuid" NOT NULL,
    "title" character varying(160) NOT NULL,
    "description" character varying(2000),
    "game_title" character varying(160),
    "game_category" character varying(50),
    "event_date" "date" NOT NULL,
    "start_time" time without time zone NOT NULL,
    "duration_minutes" integer DEFAULT 120 NOT NULL,
    "setup_minutes" integer DEFAULT 15 NOT NULL,
    "address_line1" character varying(200),
    "city" character varying(100),
    "state" character varying(50),
    "postal_code" character varying(20),
    "location_details" character varying(200),
    "difficulty_level" character varying(20),
    "max_players" integer DEFAULT 4 NOT NULL,
    "is_public" boolean DEFAULT true NOT NULL,
    "is_charity_event" boolean DEFAULT false NOT NULL,
    "status" character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "group_id" "uuid",
    "min_age" integer,
    "event_location_id" "uuid",
    "venue_hall" character varying(100),
    "venue_room" character varying(100),
    "venue_table" character varying(50),
    "host_is_playing" boolean DEFAULT true,
    "timezone" character varying(50) DEFAULT 'America/New_York'::character varying,
    "planned_games" "jsonb",
    "from_planning_session_id" "uuid",
    "is_multi_table" boolean DEFAULT false,
    "game_system" character varying(20) DEFAULT 'board_game'::character varying,
    CONSTRAINT "events_difficulty_level_check" CHECK ((("difficulty_level")::"text" = ANY ((ARRAY['beginner'::character varying, 'intermediate'::character varying, 'advanced'::character varying])::"text"[]))),
    CONSTRAINT "events_game_system_check" CHECK ((("game_system")::"text" = ANY ((ARRAY['board_game'::character varying, 'mtg'::character varying, 'pokemon_tcg'::character varying, 'yugioh'::character varying])::"text"[]))),
    CONSTRAINT "events_min_age_check" CHECK ((("min_age" >= 0) AND ("min_age" <= 100))),
    CONSTRAINT "events_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['draft'::character varying, 'published'::character varying, 'cancelled'::character varying, 'completed'::character varying])::"text"[])))
);


ALTER TABLE "public"."events" OWNER TO "postgres";


COMMENT ON COLUMN "public"."events"."event_location_id" IS 'Reference to approved venue/convention';



COMMENT ON COLUMN "public"."events"."venue_hall" IS 'Hall or area at the venue';



COMMENT ON COLUMN "public"."events"."venue_room" IS 'Room name/number at the venue';



COMMENT ON COLUMN "public"."events"."venue_table" IS 'Table number at the venue';



COMMENT ON COLUMN "public"."events"."host_is_playing" IS 'Whether the host is participating as a player. If true, they count toward max_players.';



COMMENT ON COLUMN "public"."events"."timezone" IS 'Timezone the event time is specified in (IANA timezone name)';



COMMENT ON COLUMN "public"."events"."planned_games" IS 'Array of games selected from planning session with 2+ interested players';



COMMENT ON COLUMN "public"."events"."from_planning_session_id" IS 'References the planning session this event was created from. NULL for manually created events.';



CREATE TABLE IF NOT EXISTS "public"."game_invitations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "invite_code" character varying(32) NOT NULL,
    "invited_by_user_id" "uuid" NOT NULL,
    "invited_email" character varying(255),
    "channel" character varying(20),
    "status" character varying(20) DEFAULT 'pending'::character varying,
    "accepted_by_user_id" "uuid",
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "accepted_at" timestamp without time zone,
    "expires_at" timestamp without time zone,
    CONSTRAINT "game_invitations_channel_check" CHECK ((("channel")::"text" = ANY ((ARRAY['link'::character varying, 'email'::character varying, 'facebook'::character varying, 'twitter'::character varying, 'instagram'::character varying])::"text"[]))),
    CONSTRAINT "game_invitations_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['pending'::character varying, 'accepted'::character varying, 'expired'::character varying])::"text"[])))
);


ALTER TABLE "public"."game_invitations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."game_session_registrations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "registered_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "is_host_reserved" boolean DEFAULT false
);


ALTER TABLE "public"."game_session_registrations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."group_invitation_uses" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "invitation_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "used_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."group_invitation_uses" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."group_invitations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "group_id" "uuid" NOT NULL,
    "invited_by_user_id" "uuid" NOT NULL,
    "invite_code" character varying(32) NOT NULL,
    "invited_email" character varying(255),
    "max_uses" integer DEFAULT 1,
    "uses_count" integer DEFAULT 0 NOT NULL,
    "expires_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "invited_user_id" "uuid",
    "status" character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    CONSTRAINT "group_invitations_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['pending'::character varying, 'accepted'::character varying, 'declined'::character varying, 'expired'::character varying])::"text"[])))
);


ALTER TABLE "public"."group_invitations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."group_join_requests" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "group_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "message" character varying(500),
    "status" character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    "reviewed_by_user_id" "uuid",
    "reviewed_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "group_join_requests_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::"text"[])))
);


ALTER TABLE "public"."group_join_requests" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."group_memberships" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "group_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "role" character varying(20) DEFAULT 'member'::character varying NOT NULL,
    "joined_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "group_memberships_role_check" CHECK ((("role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying, 'member'::character varying])::"text"[])))
);


ALTER TABLE "public"."group_memberships" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."groups" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "name" character varying(100) NOT NULL,
    "slug" character varying(100) NOT NULL,
    "description" character varying(2000),
    "logo_url" character varying(500),
    "cover_image_url" character varying(500),
    "group_type" character varying(20) NOT NULL,
    "location_city" character varying(100),
    "location_state" character varying(50),
    "location_radius_miles" integer,
    "created_by_user_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    "join_policy" character varying(20) DEFAULT 'open'::character varying NOT NULL,
    CONSTRAINT "groups_group_type_check" CHECK ((("group_type")::"text" = ANY ((ARRAY['geographic'::character varying, 'interest'::character varying, 'both'::character varying])::"text"[]))),
    CONSTRAINT "groups_join_policy_check" CHECK ((("join_policy")::"text" = ANY ((ARRAY['open'::character varying, 'request'::character varying, 'invite_only'::character varying])::"text"[])))
);


ALTER TABLE "public"."groups" OWNER TO "postgres";


CREATE OR REPLACE VIEW "public"."hot_locations" AS
 SELECT "el"."id",
    "el"."name",
    "el"."city",
    "el"."state",
    "el"."venue",
    "el"."start_date",
    "el"."end_date",
    "el"."is_permanent",
    "el"."recurring_days",
    "count"(DISTINCT "e"."id") AS "event_count",
    "count"(DISTINCT "er"."user_id") AS "user_count"
   FROM (("public"."event_locations" "el"
     LEFT JOIN "public"."events" "e" ON (("e"."event_location_id" = "el"."id")))
     LEFT JOIN "public"."event_registrations" "er" ON (("er"."event_id" = "e"."id")))
  WHERE ((("el"."status")::"text" = 'approved'::"text") AND (("el"."is_permanent" = true) OR ("el"."end_date" >= CURRENT_DATE) OR ("el"."recurring_days" IS NOT NULL)))
  GROUP BY "el"."id", "el"."name", "el"."city", "el"."state", "el"."venue", "el"."start_date", "el"."end_date", "el"."is_permanent", "el"."recurring_days"
  ORDER BY ("count"(DISTINCT "e"."id")) DESC, ("count"(DISTINCT "er"."user_id")) DESC
 LIMIT 10;


ALTER VIEW "public"."hot_locations" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."mtg_deck_cards" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "deck_id" "uuid" NOT NULL,
    "scryfall_id" character varying(50) NOT NULL,
    "quantity" smallint DEFAULT 1 NOT NULL,
    "board" character varying(20) DEFAULT 'main'::character varying NOT NULL,
    "card_name" character varying(200),
    "created_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "mtg_deck_cards_board_check" CHECK ((("board")::"text" = ANY ((ARRAY['main'::character varying, 'sideboard'::character varying, 'maybeboard'::character varying, 'commander'::character varying])::"text"[]))),
    CONSTRAINT "mtg_deck_cards_quantity_check" CHECK ((("quantity" >= 1) AND ("quantity" <= 99)))
);


ALTER TABLE "public"."mtg_deck_cards" OWNER TO "postgres";


COMMENT ON TABLE "public"."mtg_deck_cards" IS 'Cards in MTG decks';



CREATE TABLE IF NOT EXISTS "public"."mtg_decks" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "owner_user_id" "uuid" NOT NULL,
    "name" character varying(100) NOT NULL,
    "description" "text",
    "format_id" character varying(50),
    "commander_scryfall_id" character varying(50),
    "partner_commander_scryfall_id" character varying(50),
    "power_level" smallint,
    "is_public" boolean DEFAULT false,
    "moxfield_id" character varying(50),
    "archidekt_id" character varying(50),
    "import_url" "text",
    "card_count" integer DEFAULT 0,
    "colors" character varying(5)[] DEFAULT '{}'::character varying[],
    "estimated_price_usd" numeric(10,2),
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "mtg_decks_power_level_check" CHECK ((("power_level" >= 1) AND ("power_level" <= 10)))
);


ALTER TABLE "public"."mtg_decks" OWNER TO "postgres";


COMMENT ON TABLE "public"."mtg_decks" IS 'User MTG deck collection';



CREATE TABLE IF NOT EXISTS "public"."mtg_event_config" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "format_id" character varying(50),
    "custom_format_name" character varying(100),
    "event_type" character varying(20) DEFAULT 'casual'::character varying NOT NULL,
    "rounds_count" smallint,
    "round_time_minutes" smallint DEFAULT 50,
    "pods_size" smallint DEFAULT 4,
    "allow_proxies" boolean DEFAULT false,
    "proxy_limit" smallint,
    "power_level_min" smallint,
    "power_level_max" smallint,
    "banned_cards" "text"[] DEFAULT '{}'::"text"[],
    "packs_per_player" smallint,
    "draft_style" character varying(20),
    "cube_id" "uuid",
    "has_prizes" boolean DEFAULT false,
    "prize_structure" "text",
    "entry_fee" numeric(10,2),
    "entry_fee_currency" character varying(3) DEFAULT 'USD'::character varying,
    "require_deck_registration" boolean DEFAULT false,
    "deck_submission_deadline" timestamp with time zone,
    "allow_spectators" boolean DEFAULT true,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "mtg_event_config_draft_style_check" CHECK ((("draft_style")::"text" = ANY ((ARRAY['standard'::character varying, 'rochester'::character varying, 'winston'::character varying, 'grid'::character varying])::"text"[]))),
    CONSTRAINT "mtg_event_config_event_type_check" CHECK ((("event_type")::"text" = ANY ((ARRAY['casual'::character varying, 'swiss'::character varying, 'single_elim'::character varying, 'double_elim'::character varying, 'round_robin'::character varying, 'pods'::character varying])::"text"[]))),
    CONSTRAINT "mtg_event_config_power_level_max_check" CHECK ((("power_level_max" >= 1) AND ("power_level_max" <= 10))),
    CONSTRAINT "mtg_event_config_power_level_min_check" CHECK ((("power_level_min" >= 1) AND ("power_level_min" <= 10))),
    CONSTRAINT "valid_power_level_range" CHECK ((("power_level_min" IS NULL) OR ("power_level_max" IS NULL) OR ("power_level_min" <= "power_level_max")))
);


ALTER TABLE "public"."mtg_event_config" OWNER TO "postgres";


COMMENT ON TABLE "public"."mtg_event_config" IS 'MTG-specific configuration for events';



CREATE TABLE IF NOT EXISTS "public"."mtg_event_registrations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "registration_id" "uuid" NOT NULL,
    "deck_id" "uuid",
    "deck_snapshot" "jsonb",
    "commander_name" character varying(200),
    "commander_image_url" "text",
    "partner_commander_name" character varying(200),
    "partner_commander_image_url" "text",
    "deck_colors" character varying(5)[] DEFAULT '{}'::character varying[],
    "submitted_at" timestamp with time zone,
    "is_confirmed" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."mtg_event_registrations" OWNER TO "postgres";


COMMENT ON TABLE "public"."mtg_event_registrations" IS 'MTG-specific registration data with deck snapshots';



CREATE TABLE IF NOT EXISTS "public"."mtg_formats" (
    "id" character varying(50) NOT NULL,
    "name" character varying(100) NOT NULL,
    "description" "text",
    "min_deck_size" integer,
    "max_deck_size" integer,
    "max_copies" integer DEFAULT 4,
    "has_commander" boolean DEFAULT false,
    "has_sideboard" boolean DEFAULT true,
    "sideboard_size" integer DEFAULT 15,
    "is_constructed" boolean DEFAULT true,
    "is_active" boolean DEFAULT true,
    "sort_order" integer DEFAULT 0
);


ALTER TABLE "public"."mtg_formats" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_date_votes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "date_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "is_available" boolean NOT NULL
);


ALTER TABLE "public"."planning_date_votes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_dates" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "proposed_date" "date" NOT NULL,
    "start_time" time without time zone
);


ALTER TABLE "public"."planning_dates" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_game_suggestions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "suggested_by_user_id" "uuid" NOT NULL,
    "bgg_id" integer,
    "game_name" character varying(200) NOT NULL,
    "thumbnail_url" "text",
    "min_players" integer,
    "max_players" integer,
    "playing_time" integer,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."planning_game_suggestions" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_game_votes" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "suggestion_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL
);


ALTER TABLE "public"."planning_game_votes" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_invitees" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "has_responded" boolean DEFAULT false,
    "responded_at" timestamp without time zone,
    "cannot_attend_any" boolean DEFAULT false,
    "has_slot" boolean DEFAULT false,
    "accepted_at" timestamp with time zone
);


ALTER TABLE "public"."planning_invitees" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."planning_session_items" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "session_id" "uuid" NOT NULL,
    "item_name" character varying(100) NOT NULL,
    "item_category" character varying(20) DEFAULT 'other'::character varying NOT NULL,
    "quantity_needed" integer DEFAULT 1 NOT NULL,
    "claimed_by_user_id" "uuid",
    "claimed_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "added_by_user_id" "uuid",
    CONSTRAINT "planning_session_items_item_category_check" CHECK ((("item_category")::"text" = ANY ((ARRAY['food'::character varying, 'drinks'::character varying, 'supplies'::character varying, 'games'::character varying, 'other'::character varying])::"text"[])))
);


ALTER TABLE "public"."planning_session_items" OWNER TO "postgres";


COMMENT ON TABLE "public"."planning_session_items" IS 'Items to bring for a planning session, copied to event on finalization';



COMMENT ON COLUMN "public"."planning_session_items"."added_by_user_id" IS 'User who added this item - can delete their own items';



CREATE TABLE IF NOT EXISTS "public"."planning_sessions" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "group_id" "uuid" NOT NULL,
    "created_by_user_id" "uuid" NOT NULL,
    "title" character varying(200) NOT NULL,
    "description" "text",
    "response_deadline" timestamp without time zone NOT NULL,
    "status" character varying(20) DEFAULT 'open'::character varying,
    "finalized_date" "date",
    "finalized_game_id" "uuid",
    "created_event_id" "uuid",
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "max_participants" integer,
    "max_games" integer DEFAULT 5,
    "table_count" integer,
    "host_session_preferences" "jsonb",
    "scheduled_sessions" "jsonb",
    CONSTRAINT "check_max_participants_positive" CHECK ((("max_participants" IS NULL) OR ("max_participants" > 0))),
    CONSTRAINT "planning_sessions_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['open'::character varying, 'finalized'::character varying, 'cancelled'::character varying])::"text"[])))
);


ALTER TABLE "public"."planning_sessions" OWNER TO "postgres";


COMMENT ON COLUMN "public"."planning_sessions"."max_games" IS 'Maximum number of game suggestions allowed (based on host tier)';



CREATE TABLE IF NOT EXISTS "public"."player_requests" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "description" character varying(1000),
    "player_count_needed" integer DEFAULT 1,
    "is_active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "expires_at" timestamp without time zone,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "status" character varying(20) DEFAULT 'open'::character varying,
    CONSTRAINT "player_requests_status_check" CHECK ((("status")::"text" = ANY ((ARRAY['open'::character varying, 'filled'::character varying, 'cancelled'::character varying])::"text"[])))
);


ALTER TABLE "public"."player_requests" OWNER TO "postgres";


COMMENT ON TABLE "public"."player_requests" IS 'Urgent requests from event hosts needing fill-in players (max 15 min duration)';



CREATE TABLE IF NOT EXISTS "public"."pokemon_cards_cache" (
    "pokemon_tcg_id" character varying(50) NOT NULL,
    "name" character varying(200) NOT NULL,
    "supertype" character varying(50) NOT NULL,
    "subtypes" character varying(50)[] DEFAULT '{}'::character varying[],
    "hp" character varying(10),
    "types" character varying(20)[] DEFAULT '{}'::character varying[],
    "evolves_from" character varying(100),
    "evolves_to" character varying(100)[] DEFAULT '{}'::character varying[],
    "abilities" "jsonb" DEFAULT '[]'::"jsonb",
    "attacks" "jsonb" DEFAULT '[]'::"jsonb",
    "weaknesses" "jsonb" DEFAULT '[]'::"jsonb",
    "resistances" "jsonb" DEFAULT '[]'::"jsonb",
    "retreat_cost" character varying(20)[] DEFAULT '{}'::character varying[],
    "set_id" character varying(50) NOT NULL,
    "set_name" character varying(200) NOT NULL,
    "set_series" character varying(100),
    "card_number" character varying(20) NOT NULL,
    "artist" character varying(200),
    "rarity" character varying(50),
    "flavor_text" "text",
    "national_pokedex_numbers" integer[] DEFAULT '{}'::integer[],
    "legalities" "jsonb" DEFAULT '{}'::"jsonb",
    "regulation_mark" character varying(10),
    "image_small" "text" NOT NULL,
    "image_large" "text" NOT NULL,
    "tcgplayer_url" "text",
    "tcgplayer_prices" "jsonb",
    "cardmarket_url" "text",
    "cardmarket_prices" "jsonb",
    "cached_at" timestamp with time zone DEFAULT "now"(),
    "stale_at" timestamp with time zone DEFAULT ("now"() + '24:00:00'::interval),
    "search_vector" "tsvector"
);


ALTER TABLE "public"."pokemon_cards_cache" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_cards_cache" IS 'Cache of Pokemon TCG card data from pokemontcg.io API';



CREATE TABLE IF NOT EXISTS "public"."pokemon_deck_cards" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "deck_id" "uuid" NOT NULL,
    "pokemon_tcg_id" character varying(50) NOT NULL,
    "quantity" smallint DEFAULT 1 NOT NULL,
    "card_name" character varying(200),
    "supertype" character varying(50),
    "created_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "pokemon_deck_cards_quantity_check" CHECK ((("quantity" >= 1) AND ("quantity" <= 60)))
);


ALTER TABLE "public"."pokemon_deck_cards" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_deck_cards" IS 'Cards in Pokemon TCG decks';



CREATE TABLE IF NOT EXISTS "public"."pokemon_decks" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "owner_user_id" "uuid" NOT NULL,
    "name" character varying(100) NOT NULL,
    "description" "text",
    "format_id" character varying(50),
    "featured_pokemon_tcg_id" character varying(50),
    "featured_pokemon_name" character varying(200),
    "featured_pokemon_image" "text",
    "archetype" character varying(100),
    "is_public" boolean DEFAULT false,
    "pokemon_tcg_live_code" character varying(50),
    "limitless_tcg_id" character varying(50),
    "import_url" "text",
    "card_count" integer DEFAULT 0,
    "pokemon_count" integer DEFAULT 0,
    "trainer_count" integer DEFAULT 0,
    "energy_count" integer DEFAULT 0,
    "estimated_price_usd" numeric(10,2),
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."pokemon_decks" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_decks" IS 'User Pokemon TCG deck collection';



CREATE TABLE IF NOT EXISTS "public"."pokemon_event_config" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "event_id" "uuid" NOT NULL,
    "format_id" character varying(50),
    "custom_format_name" character varying(100),
    "event_type" character varying(30) DEFAULT 'casual'::character varying NOT NULL,
    "tournament_style" character varying(20),
    "rounds_count" smallint,
    "round_time_minutes" smallint DEFAULT 50,
    "best_of" smallint DEFAULT 1,
    "top_cut" smallint,
    "allow_proxies" boolean DEFAULT false,
    "proxy_limit" smallint,
    "require_deck_registration" boolean DEFAULT false,
    "deck_submission_deadline" timestamp with time zone,
    "allow_deck_changes" boolean DEFAULT false,
    "has_prizes" boolean DEFAULT false,
    "prize_structure" "text",
    "entry_fee" numeric(10,2),
    "entry_fee_currency" character varying(3) DEFAULT 'USD'::character varying,
    "use_play_points" boolean DEFAULT false,
    "has_junior_division" boolean DEFAULT false,
    "has_senior_division" boolean DEFAULT false,
    "has_masters_division" boolean DEFAULT true,
    "allow_spectators" boolean DEFAULT true,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "pokemon_event_config_best_of_check" CHECK (("best_of" = ANY (ARRAY[1, 3]))),
    CONSTRAINT "pokemon_event_config_event_type_check" CHECK ((("event_type")::"text" = ANY ((ARRAY['casual'::character varying, 'league'::character varying, 'league_cup'::character varying, 'league_challenge'::character varying, 'regional'::character varying, 'international'::character varying, 'worlds'::character varying, 'prerelease'::character varying, 'draft'::character varying])::"text"[]))),
    CONSTRAINT "pokemon_event_config_tournament_style_check" CHECK ((("tournament_style")::"text" = ANY ((ARRAY['swiss'::character varying, 'single_elim'::character varying, 'double_elim'::character varying])::"text"[])))
);


ALTER TABLE "public"."pokemon_event_config" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_event_config" IS 'Pokemon TCG-specific configuration for events';



CREATE TABLE IF NOT EXISTS "public"."pokemon_event_registrations" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "registration_id" "uuid" NOT NULL,
    "deck_id" "uuid",
    "deck_snapshot" "jsonb",
    "featured_pokemon_name" character varying(200),
    "featured_pokemon_image" "text",
    "deck_archetype" character varying(100),
    "age_division" character varying(20),
    "submitted_at" timestamp with time zone,
    "is_confirmed" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "pokemon_event_registrations_age_division_check" CHECK ((("age_division")::"text" = ANY ((ARRAY['junior'::character varying, 'senior'::character varying, 'masters'::character varying])::"text"[])))
);


ALTER TABLE "public"."pokemon_event_registrations" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_event_registrations" IS 'Pokemon TCG-specific registration data with deck snapshots';



CREATE TABLE IF NOT EXISTS "public"."pokemon_formats" (
    "id" character varying(50) NOT NULL,
    "name" character varying(100) NOT NULL,
    "description" "text",
    "min_deck_size" integer DEFAULT 60,
    "max_deck_size" integer DEFAULT 60,
    "max_copies" integer DEFAULT 4,
    "is_rotating" boolean DEFAULT false,
    "is_active" boolean DEFAULT true,
    "sort_order" integer DEFAULT 0
);


ALTER TABLE "public"."pokemon_formats" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_formats" IS 'Pokemon TCG format definitions';



CREATE TABLE IF NOT EXISTS "public"."pokemon_sets_cache" (
    "set_id" character varying(50) NOT NULL,
    "name" character varying(200) NOT NULL,
    "series" character varying(100) NOT NULL,
    "printed_total" integer NOT NULL,
    "total" integer NOT NULL,
    "ptcgo_code" character varying(20),
    "release_date" character varying(20),
    "legalities" "jsonb" DEFAULT '{}'::"jsonb",
    "symbol_url" "text" NOT NULL,
    "logo_url" "text" NOT NULL,
    "cached_at" timestamp with time zone DEFAULT "now"(),
    "stale_at" timestamp with time zone DEFAULT ("now"() + '24:00:00'::interval)
);


ALTER TABLE "public"."pokemon_sets_cache" OWNER TO "postgres";


COMMENT ON TABLE "public"."pokemon_sets_cache" IS 'Cache of Pokemon TCG set data from pokemontcg.io API';



CREATE TABLE IF NOT EXISTS "public"."raffle_entries" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "raffle_id" "uuid" NOT NULL,
    "user_id" "uuid" NOT NULL,
    "entry_type" character varying(30) NOT NULL,
    "source_id" "uuid",
    "entry_count" integer DEFAULT 1 NOT NULL,
    "mail_in_name" character varying(200),
    "mail_in_address" "text",
    "mail_in_verified" boolean DEFAULT false,
    "created_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "positive_entries" CHECK (("entry_count" > 0)),
    CONSTRAINT "valid_entry_type" CHECK ((("entry_type")::"text" = ANY ((ARRAY['host_event'::character varying, 'plan_session'::character varying, 'attend_event'::character varying, 'mail_in'::character varying])::"text"[])))
);


ALTER TABLE "public"."raffle_entries" OWNER TO "postgres";


COMMENT ON TABLE "public"."raffle_entries" IS 'User entries in raffles earned through participation';



COMMENT ON COLUMN "public"."raffle_entries"."entry_type" IS 'How entry was earned: host_event, plan_session, attend_event, mail_in';



COMMENT ON COLUMN "public"."raffle_entries"."entry_count" IS 'Number of entries (1 for free, 2 for paid on host/plan)';



CREATE TABLE IF NOT EXISTS "public"."raffles" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "title" character varying(200) NOT NULL,
    "description" "text",
    "prize_name" character varying(200) NOT NULL,
    "prize_description" "text",
    "prize_image_url" "text",
    "prize_bgg_id" integer,
    "prize_value_cents" integer,
    "start_date" timestamp with time zone NOT NULL,
    "end_date" timestamp with time zone NOT NULL,
    "terms_conditions" "text",
    "mail_in_instructions" "text",
    "status" character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    "winner_user_id" "uuid",
    "winner_selected_at" timestamp with time zone,
    "winner_notified_at" timestamp with time zone,
    "winner_claimed_at" timestamp with time zone,
    "banner_image_url" "text",
    "created_by_user_id" "uuid" NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"(),
    "updated_at" timestamp with time zone DEFAULT "now"(),
    CONSTRAINT "valid_dates" CHECK (("end_date" > "start_date")),
    CONSTRAINT "valid_status" CHECK ((("status")::"text" = ANY ((ARRAY['draft'::character varying, 'active'::character varying, 'ended'::character varying, 'cancelled'::character varying])::"text"[])))
);


ALTER TABLE "public"."raffles" OWNER TO "postgres";


COMMENT ON TABLE "public"."raffles" IS 'Monthly raffles with prizes for active users';



COMMENT ON COLUMN "public"."raffles"."mail_in_instructions" IS 'Instructions for "No purchase necessary" mail-in entry (legal compliance)';



COMMENT ON COLUMN "public"."raffles"."banner_image_url" IS 'Optional custom banner image URL for the raffle (displayed on home page)';



CREATE TABLE IF NOT EXISTS "public"."recurring_games" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "group_id" "uuid" NOT NULL,
    "title" character varying(160) NOT NULL,
    "description" character varying(2000),
    "day_of_week" integer NOT NULL,
    "start_time" time without time zone NOT NULL,
    "duration_minutes" integer DEFAULT 120 NOT NULL,
    "max_players" integer DEFAULT 4 NOT NULL,
    "location_details" character varying(200),
    "is_active" boolean DEFAULT true NOT NULL,
    "created_at" timestamp with time zone DEFAULT "now"() NOT NULL,
    CONSTRAINT "recurring_games_day_of_week_check" CHECK ((("day_of_week" >= 0) AND ("day_of_week" <= 6)))
);


ALTER TABLE "public"."recurring_games" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."scryfall_cards_cache" (
    "scryfall_id" "uuid" NOT NULL,
    "oracle_id" "uuid",
    "name" character varying(200) NOT NULL,
    "mana_cost" character varying(100),
    "cmc" numeric(4,1),
    "type_line" character varying(200),
    "oracle_text" "text",
    "power" character varying(10),
    "toughness" character varying(10),
    "loyalty" character varying(10),
    "colors" "text"[],
    "color_identity" "text"[],
    "keywords" "text"[],
    "legalities" "jsonb",
    "set_code" character varying(10),
    "set_name" character varying(100),
    "collector_number" character varying(20),
    "rarity" character varying(20),
    "image_uri_small" character varying(500),
    "image_uri_normal" character varying(500),
    "image_uri_large" character varying(500),
    "image_uri_art_crop" character varying(500),
    "image_uri_png" character varying(500),
    "prices" "jsonb",
    "is_double_faced" boolean DEFAULT false,
    "card_faces" "jsonb",
    "layout" character varying(30),
    "cached_at" timestamp with time zone DEFAULT "now"() NOT NULL
);


ALTER TABLE "public"."scryfall_cards_cache" OWNER TO "postgres";


CREATE TABLE IF NOT EXISTS "public"."stripe_invoices" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "stripe_invoice_id" character varying(255) NOT NULL,
    "stripe_subscription_id" character varying(255),
    "amount_cents" integer NOT NULL,
    "tax_cents" integer DEFAULT 0,
    "currency" character varying(3) DEFAULT 'usd'::character varying,
    "status" character varying(20) NOT NULL,
    "invoice_date" timestamp with time zone NOT NULL,
    "period_start" timestamp with time zone,
    "period_end" timestamp with time zone,
    "hosted_invoice_url" "text",
    "invoice_pdf_url" "text",
    "payment_method_brand" character varying(20),
    "payment_method_last4" character varying(4),
    "created_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."stripe_invoices" OWNER TO "postgres";


COMMENT ON TABLE "public"."stripe_invoices" IS 'Cached invoice data from Stripe for display in profile';



CREATE TABLE IF NOT EXISTS "public"."subscription_events" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "user_id" "uuid" NOT NULL,
    "event_type" character varying(50) NOT NULL,
    "old_tier" character varying(20),
    "new_tier" character varying(20),
    "stripe_event_id" character varying(255),
    "admin_user_id" "uuid",
    "notes" "text",
    "created_at" timestamp with time zone DEFAULT "now"()
);


ALTER TABLE "public"."subscription_events" OWNER TO "postgres";


COMMENT ON TABLE "public"."subscription_events" IS 'Audit log of all subscription changes';



CREATE TABLE IF NOT EXISTS "public"."users" (
    "id" "uuid" DEFAULT "gen_random_uuid"() NOT NULL,
    "firebase_uid" character varying(128) NOT NULL,
    "email" character varying(255) NOT NULL,
    "display_name" character varying(120),
    "avatar_url" character varying(500),
    "subscription_tier" character varying(20) DEFAULT 'free'::character varying NOT NULL,
    "subscription_expires_at" timestamp without time zone,
    "created_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "updated_at" timestamp without time zone DEFAULT "now"() NOT NULL,
    "max_travel_miles" integer DEFAULT 25,
    "home_city" character varying(100),
    "home_state" character varying(50),
    "home_postal_code" character varying(20),
    "bio" character varying(500),
    "favorite_games" "text"[],
    "preferred_game_types" "text"[],
    "is_admin" boolean DEFAULT false NOT NULL,
    "blocked_user_ids" "uuid"[] DEFAULT '{}'::"uuid"[],
    "birth_year" integer,
    "username" character varying(30) NOT NULL,
    "is_suspended" boolean DEFAULT false,
    "suspension_reason" "text",
    "suspended_at" timestamp with time zone,
    "suspended_by_user_id" "uuid",
    "active_city" character varying(100),
    "active_state" character varying(50),
    "active_location_expires_at" timestamp with time zone,
    "active_event_location_id" "uuid",
    "active_location_hall" character varying(100),
    "active_location_room" character varying(100),
    "active_location_table" character varying(50),
    "stripe_customer_id" character varying(255),
    "stripe_subscription_id" character varying(255),
    "subscription_status" character varying(20) DEFAULT 'active'::character varying,
    "account_status" character varying(20) DEFAULT 'active'::character varying,
    "banned_at" timestamp with time zone,
    "banned_by_user_id" "uuid",
    "ban_reason" "text",
    "subscription_override_tier" character varying(20),
    "subscription_override_reason" "text",
    "subscription_override_by_user_id" "uuid",
    "subscription_override_at" timestamp with time zone,
    "timezone" character varying(50) DEFAULT 'America/Phoenix'::character varying,
    "is_founding_member" boolean DEFAULT false,
    "auth_provider" "text" DEFAULT 'password'::"text",
    "password_changed_at" timestamp with time zone,
    "is_test_account" boolean DEFAULT false,
    CONSTRAINT "chk_username_format" CHECK ((("username")::"text" ~ '^[a-zA-Z][a-zA-Z0-9_]{2,29}$'::"text")),
    CONSTRAINT "users_birth_year_check" CHECK ((("birth_year" >= 1900) AND ("birth_year" <= 2100))),
    CONSTRAINT "users_subscription_tier_check" CHECK ((("subscription_tier")::"text" = ANY ((ARRAY['free'::character varying, 'basic'::character varying, 'pro'::character varying, 'premium'::character varying])::"text"[])))
);


ALTER TABLE "public"."users" OWNER TO "postgres";


COMMENT ON COLUMN "public"."users"."is_suspended" IS 'Whether the user account is suspended';



COMMENT ON COLUMN "public"."users"."suspension_reason" IS 'Admin notes about why the user was suspended';



COMMENT ON COLUMN "public"."users"."suspended_at" IS 'When the user was suspended';



COMMENT ON COLUMN "public"."users"."suspended_by_user_id" IS 'Which admin suspended the user';



COMMENT ON COLUMN "public"."users"."active_city" IS 'Temporary active location city (e.g., when at a convention)';



COMMENT ON COLUMN "public"."users"."active_state" IS 'Temporary active location state';



COMMENT ON COLUMN "public"."users"."active_location_expires_at" IS 'When the active location expires (optional, for auto-clearing)';



COMMENT ON COLUMN "public"."users"."active_event_location_id" IS 'Reference to venue/convention the user is currently at';



COMMENT ON COLUMN "public"."users"."active_location_hall" IS 'Hall or area at the venue';



COMMENT ON COLUMN "public"."users"."active_location_room" IS 'Room name/number at the venue';



COMMENT ON COLUMN "public"."users"."active_location_table" IS 'Table number at the venue';



COMMENT ON COLUMN "public"."users"."stripe_customer_id" IS 'Stripe customer ID for billing';



COMMENT ON COLUMN "public"."users"."stripe_subscription_id" IS 'Active Stripe subscription ID';



COMMENT ON COLUMN "public"."users"."subscription_status" IS 'Stripe subscription status: active, past_due, canceled, incomplete';



COMMENT ON COLUMN "public"."users"."account_status" IS 'Account status: active, suspended (temporary), banned (permanent)';



COMMENT ON COLUMN "public"."users"."subscription_override_tier" IS 'Admin-set tier that overrides Stripe subscription';



COMMENT ON COLUMN "public"."users"."timezone" IS 'User''s preferred timezone (IANA timezone name, e.g. America/New_York)';



COMMENT ON COLUMN "public"."users"."is_founding_member" IS 'Flag for users who joined during the founding period - displays special badge';



COMMENT ON COLUMN "public"."users"."auth_provider" IS 'Firebase sign-in provider: password, google.com, facebook.com, etc.';



COMMENT ON COLUMN "public"."users"."is_test_account" IS 'Test accounts used for E2E testing - excluded from raffles';



CREATE TABLE IF NOT EXISTS "public"."zip_codes" (
    "zip" character varying(10) NOT NULL,
    "city" character varying(100) NOT NULL,
    "state" character varying(2) NOT NULL,
    "latitude" numeric(10,7) NOT NULL,
    "longitude" numeric(10,7) NOT NULL,
    "timezone" character varying(50)
);


ALTER TABLE "public"."zip_codes" OWNER TO "postgres";


COMMENT ON TABLE "public"."zip_codes" IS 'US zip codes with coordinates for radius-based event searching';



ALTER TABLE ONLY "public"."ad_clicks"
    ADD CONSTRAINT "ad_clicks_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."ad_impressions"
    ADD CONSTRAINT "ad_impressions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."admin_bugs"
    ADD CONSTRAINT "admin_bugs_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."admin_notes"
    ADD CONSTRAINT "admin_notes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."ads"
    ADD CONSTRAINT "ads_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."bgg_games_cache"
    ADD CONSTRAINT "bgg_games_cache_pkey" PRIMARY KEY ("bgg_id");



ALTER TABLE ONLY "public"."chat_messages"
    ADD CONSTRAINT "chat_messages_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."chat_moderation_actions"
    ADD CONSTRAINT "chat_moderation_actions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."chat_reports"
    ADD CONSTRAINT "chat_reports_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."contact_submissions"
    ADD CONSTRAINT "contact_submissions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_game_sessions"
    ADD CONSTRAINT "event_game_sessions_event_id_table_id_slot_index_key" UNIQUE ("event_id", "table_id", "slot_index");



ALTER TABLE ONLY "public"."event_game_sessions"
    ADD CONSTRAINT "event_game_sessions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_games"
    ADD CONSTRAINT "event_games_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_items"
    ADD CONSTRAINT "event_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_locations"
    ADD CONSTRAINT "event_locations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_registrations"
    ADD CONSTRAINT "event_registrations_event_id_user_id_key" UNIQUE ("event_id", "user_id");



ALTER TABLE ONLY "public"."event_registrations"
    ADD CONSTRAINT "event_registrations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."event_tables"
    ADD CONSTRAINT "event_tables_event_id_table_number_key" UNIQUE ("event_id", "table_number");



ALTER TABLE ONLY "public"."event_tables"
    ADD CONSTRAINT "event_tables_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."events"
    ADD CONSTRAINT "events_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."game_invitations"
    ADD CONSTRAINT "game_invitations_invite_code_key" UNIQUE ("invite_code");



ALTER TABLE ONLY "public"."game_invitations"
    ADD CONSTRAINT "game_invitations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."game_session_registrations"
    ADD CONSTRAINT "game_session_registrations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."game_session_registrations"
    ADD CONSTRAINT "game_session_registrations_session_id_user_id_key" UNIQUE ("session_id", "user_id");



ALTER TABLE ONLY "public"."group_invitation_uses"
    ADD CONSTRAINT "group_invitation_uses_invitation_id_user_id_key" UNIQUE ("invitation_id", "user_id");



ALTER TABLE ONLY "public"."group_invitation_uses"
    ADD CONSTRAINT "group_invitation_uses_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."group_invitations"
    ADD CONSTRAINT "group_invitations_invite_code_key" UNIQUE ("invite_code");



ALTER TABLE ONLY "public"."group_invitations"
    ADD CONSTRAINT "group_invitations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."group_join_requests"
    ADD CONSTRAINT "group_join_requests_group_id_user_id_key" UNIQUE ("group_id", "user_id");



ALTER TABLE ONLY "public"."group_join_requests"
    ADD CONSTRAINT "group_join_requests_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."group_memberships"
    ADD CONSTRAINT "group_memberships_group_id_user_id_key" UNIQUE ("group_id", "user_id");



ALTER TABLE ONLY "public"."group_memberships"
    ADD CONSTRAINT "group_memberships_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."groups"
    ADD CONSTRAINT "groups_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."groups"
    ADD CONSTRAINT "groups_slug_key" UNIQUE ("slug");



ALTER TABLE ONLY "public"."mtg_deck_cards"
    ADD CONSTRAINT "mtg_deck_cards_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_decks"
    ADD CONSTRAINT "mtg_decks_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_event_config"
    ADD CONSTRAINT "mtg_event_config_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_event_registrations"
    ADD CONSTRAINT "mtg_event_registrations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_formats"
    ADD CONSTRAINT "mtg_formats_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_event_config"
    ADD CONSTRAINT "one_config_per_event" UNIQUE ("event_id");



ALTER TABLE ONLY "public"."mtg_event_registrations"
    ADD CONSTRAINT "one_mtg_reg_per_registration" UNIQUE ("registration_id");



ALTER TABLE ONLY "public"."pokemon_event_config"
    ADD CONSTRAINT "one_pokemon_config_per_event" UNIQUE ("event_id");



ALTER TABLE ONLY "public"."pokemon_event_registrations"
    ADD CONSTRAINT "one_pokemon_reg_per_registration" UNIQUE ("registration_id");



ALTER TABLE ONLY "public"."planning_date_votes"
    ADD CONSTRAINT "planning_date_votes_date_id_user_id_key" UNIQUE ("date_id", "user_id");



ALTER TABLE ONLY "public"."planning_date_votes"
    ADD CONSTRAINT "planning_date_votes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_dates"
    ADD CONSTRAINT "planning_dates_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_dates"
    ADD CONSTRAINT "planning_dates_session_id_proposed_date_key" UNIQUE ("session_id", "proposed_date");



ALTER TABLE ONLY "public"."planning_game_suggestions"
    ADD CONSTRAINT "planning_game_suggestions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_game_suggestions"
    ADD CONSTRAINT "planning_game_suggestions_session_id_bgg_id_key" UNIQUE ("session_id", "bgg_id");



ALTER TABLE ONLY "public"."planning_game_votes"
    ADD CONSTRAINT "planning_game_votes_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_game_votes"
    ADD CONSTRAINT "planning_game_votes_suggestion_id_user_id_key" UNIQUE ("suggestion_id", "user_id");



ALTER TABLE ONLY "public"."planning_invitees"
    ADD CONSTRAINT "planning_invitees_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_invitees"
    ADD CONSTRAINT "planning_invitees_session_id_user_id_key" UNIQUE ("session_id", "user_id");



ALTER TABLE ONLY "public"."planning_session_items"
    ADD CONSTRAINT "planning_session_items_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."planning_sessions"
    ADD CONSTRAINT "planning_sessions_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."player_requests"
    ADD CONSTRAINT "player_requests_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_cards_cache"
    ADD CONSTRAINT "pokemon_cards_cache_pkey" PRIMARY KEY ("pokemon_tcg_id");



ALTER TABLE ONLY "public"."pokemon_deck_cards"
    ADD CONSTRAINT "pokemon_deck_cards_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_decks"
    ADD CONSTRAINT "pokemon_decks_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_event_config"
    ADD CONSTRAINT "pokemon_event_config_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_event_registrations"
    ADD CONSTRAINT "pokemon_event_registrations_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_formats"
    ADD CONSTRAINT "pokemon_formats_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."pokemon_sets_cache"
    ADD CONSTRAINT "pokemon_sets_cache_pkey" PRIMARY KEY ("set_id");



ALTER TABLE ONLY "public"."raffle_entries"
    ADD CONSTRAINT "raffle_entries_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."raffles"
    ADD CONSTRAINT "raffles_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."recurring_games"
    ADD CONSTRAINT "recurring_games_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."scryfall_cards_cache"
    ADD CONSTRAINT "scryfall_cards_cache_pkey" PRIMARY KEY ("scryfall_id");



ALTER TABLE ONLY "public"."stripe_invoices"
    ADD CONSTRAINT "stripe_invoices_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."stripe_invoices"
    ADD CONSTRAINT "stripe_invoices_stripe_invoice_id_key" UNIQUE ("stripe_invoice_id");



ALTER TABLE ONLY "public"."subscription_events"
    ADD CONSTRAINT "subscription_events_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."mtg_deck_cards"
    ADD CONSTRAINT "unique_card_per_board" UNIQUE ("deck_id", "scryfall_id", "board");



ALTER TABLE ONLY "public"."pokemon_deck_cards"
    ADD CONSTRAINT "unique_pokemon_card_per_deck" UNIQUE ("deck_id", "pokemon_tcg_id");



ALTER TABLE ONLY "public"."chat_reports"
    ADD CONSTRAINT "unique_report_per_user" UNIQUE ("message_id", "reporter_id");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_email_key" UNIQUE ("email");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_firebase_uid_key" UNIQUE ("firebase_uid");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_pkey" PRIMARY KEY ("id");



ALTER TABLE ONLY "public"."zip_codes"
    ADD CONSTRAINT "zip_codes_pkey" PRIMARY KEY ("zip");



CREATE INDEX "idx_ad_clicks_ad_id" ON "public"."ad_clicks" USING "btree" ("ad_id");



CREATE INDEX "idx_ad_clicks_date" ON "public"."ad_clicks" USING "btree" ("created_at");



CREATE INDEX "idx_ad_impressions_ad_id" ON "public"."ad_impressions" USING "btree" ("ad_id");



CREATE INDEX "idx_ad_impressions_date" ON "public"."ad_impressions" USING "btree" ("created_at");



CREATE INDEX "idx_admin_bugs_priority" ON "public"."admin_bugs" USING "btree" ("priority");



CREATE INDEX "idx_admin_bugs_status" ON "public"."admin_bugs" USING "btree" ("status");



CREATE INDEX "idx_admin_notes_category" ON "public"."admin_notes" USING "btree" ("category");



CREATE INDEX "idx_admin_notes_pinned" ON "public"."admin_notes" USING "btree" ("is_pinned") WHERE ("is_pinned" = true);



CREATE INDEX "idx_ads_active" ON "public"."ads" USING "btree" ("is_active", "start_date", "end_date");



CREATE INDEX "idx_ads_placement" ON "public"."ads" USING "btree" ("placement");



CREATE INDEX "idx_bgg_cache_name" ON "public"."bgg_games_cache" USING "btree" ("name");



CREATE INDEX "idx_bgg_cache_name_trgm" ON "public"."bgg_games_cache" USING "gin" ("name" "public"."gin_trgm_ops");



CREATE INDEX "idx_bgg_cache_rank" ON "public"."bgg_games_cache" USING "btree" ("bgg_rank") WHERE ("bgg_rank" IS NOT NULL);



CREATE INDEX "idx_bgg_cache_ratings" ON "public"."bgg_games_cache" USING "btree" ("num_ratings" DESC);



CREATE INDEX "idx_bgg_cache_search" ON "public"."bgg_games_cache" USING "gin" ("search_vector");



CREATE INDEX "idx_chat_messages_context" ON "public"."chat_messages" USING "btree" ("context_type", "context_id");



CREATE INDEX "idx_chat_messages_created" ON "public"."chat_messages" USING "btree" ("context_id", "created_at" DESC);



CREATE INDEX "idx_chat_messages_user" ON "public"."chat_messages" USING "btree" ("user_id");



CREATE INDEX "idx_chat_moderation_expires" ON "public"."chat_moderation_actions" USING "btree" ("expires_at") WHERE ("expires_at" IS NOT NULL);



CREATE INDEX "idx_chat_moderation_user" ON "public"."chat_moderation_actions" USING "btree" ("user_id");



CREATE INDEX "idx_chat_reports_created" ON "public"."chat_reports" USING "btree" ("created_at" DESC);



CREATE INDEX "idx_chat_reports_message" ON "public"."chat_reports" USING "btree" ("message_id");



CREATE INDEX "idx_chat_reports_status" ON "public"."chat_reports" USING "btree" ("status");



CREATE INDEX "idx_contact_submissions_created" ON "public"."contact_submissions" USING "btree" ("created_at" DESC);



CREATE INDEX "idx_contact_submissions_status" ON "public"."contact_submissions" USING "btree" ("status");



CREATE INDEX "idx_event_game_sessions_event" ON "public"."event_game_sessions" USING "btree" ("event_id");



CREATE INDEX "idx_event_game_sessions_table" ON "public"."event_game_sessions" USING "btree" ("table_id");



CREATE INDEX "idx_event_games_bgg" ON "public"."event_games" USING "btree" ("bgg_id");



CREATE INDEX "idx_event_games_event" ON "public"."event_games" USING "btree" ("event_id");



CREATE INDEX "idx_event_locations_dates" ON "public"."event_locations" USING "btree" ("start_date", "end_date");



CREATE INDEX "idx_event_locations_normalized" ON "public"."event_locations" USING "btree" ("name_normalized", "start_date", "end_date");



CREATE INDEX "idx_event_locations_permanent" ON "public"."event_locations" USING "btree" ("is_permanent") WHERE ("is_permanent" = true);



CREATE INDEX "idx_event_locations_postal_code" ON "public"."event_locations" USING "btree" ("postal_code");



CREATE INDEX "idx_event_locations_recurring" ON "public"."event_locations" USING "gin" ("recurring_days") WHERE ("recurring_days" IS NOT NULL);



CREATE INDEX "idx_event_locations_status" ON "public"."event_locations" USING "btree" ("status", "end_date");



CREATE INDEX "idx_event_tables_event" ON "public"."event_tables" USING "btree" ("event_id");



CREATE INDEX "idx_events_event_location" ON "public"."events" USING "btree" ("event_location_id") WHERE ("event_location_id" IS NOT NULL);



CREATE INDEX "idx_events_game_system" ON "public"."events" USING "btree" ("game_system");



CREATE INDEX "idx_events_group" ON "public"."events" USING "btree" ("group_id");



CREATE INDEX "idx_events_group_planning" ON "public"."events" USING "btree" ("group_id", "from_planning_session_id") WHERE ("from_planning_session_id" IS NOT NULL);



CREATE INDEX "idx_events_min_age" ON "public"."events" USING "btree" ("min_age") WHERE ("min_age" IS NOT NULL);



CREATE INDEX "idx_events_timezone" ON "public"."events" USING "btree" ("timezone");



CREATE INDEX "idx_game_session_registrations_session" ON "public"."game_session_registrations" USING "btree" ("session_id");



CREATE INDEX "idx_game_session_registrations_user" ON "public"."game_session_registrations" USING "btree" ("user_id");



CREATE INDEX "idx_group_memberships_group" ON "public"."group_memberships" USING "btree" ("group_id");



CREATE INDEX "idx_group_memberships_user" ON "public"."group_memberships" USING "btree" ("user_id");



CREATE INDEX "idx_groups_join_policy" ON "public"."groups" USING "btree" ("join_policy");



CREATE INDEX "idx_groups_slug" ON "public"."groups" USING "btree" ("slug");



CREATE INDEX "idx_groups_type" ON "public"."groups" USING "btree" ("group_type");



CREATE INDEX "idx_grp_invitations_code" ON "public"."group_invitations" USING "btree" ("invite_code");



CREATE INDEX "idx_grp_invitations_group" ON "public"."group_invitations" USING "btree" ("group_id");



CREATE INDEX "idx_grp_invitations_invited_user" ON "public"."group_invitations" USING "btree" ("invited_user_id") WHERE ("invited_user_id" IS NOT NULL);



CREATE INDEX "idx_grp_invitations_status" ON "public"."group_invitations" USING "btree" ("status");



CREATE UNIQUE INDEX "idx_grp_invitations_user_group_unique" ON "public"."group_invitations" USING "btree" ("group_id", "invited_user_id") WHERE (("invited_user_id" IS NOT NULL) AND (("status")::"text" = 'pending'::"text"));



CREATE INDEX "idx_invitation_uses_invitation" ON "public"."group_invitation_uses" USING "btree" ("invitation_id");



CREATE INDEX "idx_invitation_uses_user" ON "public"."group_invitation_uses" USING "btree" ("user_id");



CREATE INDEX "idx_invitations_code" ON "public"."game_invitations" USING "btree" ("invite_code");



CREATE INDEX "idx_invitations_event" ON "public"."game_invitations" USING "btree" ("event_id");



CREATE INDEX "idx_join_requests_group" ON "public"."group_join_requests" USING "btree" ("group_id");



CREATE INDEX "idx_join_requests_status" ON "public"."group_join_requests" USING "btree" ("group_id", "status");



CREATE INDEX "idx_join_requests_user" ON "public"."group_join_requests" USING "btree" ("user_id");



CREATE INDEX "idx_mtg_deck_cards_deck" ON "public"."mtg_deck_cards" USING "btree" ("deck_id");



CREATE INDEX "idx_mtg_deck_cards_scryfall" ON "public"."mtg_deck_cards" USING "btree" ("scryfall_id");



CREATE INDEX "idx_mtg_decks_commander" ON "public"."mtg_decks" USING "btree" ("commander_scryfall_id") WHERE ("commander_scryfall_id" IS NOT NULL);



CREATE INDEX "idx_mtg_decks_format" ON "public"."mtg_decks" USING "btree" ("format_id");



CREATE INDEX "idx_mtg_decks_owner" ON "public"."mtg_decks" USING "btree" ("owner_user_id");



CREATE INDEX "idx_mtg_decks_public" ON "public"."mtg_decks" USING "btree" ("is_public") WHERE ("is_public" = true);



CREATE INDEX "idx_mtg_event_config_event_id" ON "public"."mtg_event_config" USING "btree" ("event_id");



CREATE INDEX "idx_mtg_event_config_format_id" ON "public"."mtg_event_config" USING "btree" ("format_id");



CREATE INDEX "idx_mtg_event_reg_deck" ON "public"."mtg_event_registrations" USING "btree" ("deck_id");



CREATE INDEX "idx_mtg_event_reg_registration" ON "public"."mtg_event_registrations" USING "btree" ("registration_id");



CREATE INDEX "idx_planning_date_votes_date" ON "public"."planning_date_votes" USING "btree" ("date_id");



CREATE INDEX "idx_planning_dates_session" ON "public"."planning_dates" USING "btree" ("session_id");



CREATE INDEX "idx_planning_game_suggestions_session" ON "public"."planning_game_suggestions" USING "btree" ("session_id");



CREATE INDEX "idx_planning_game_votes_suggestion" ON "public"."planning_game_votes" USING "btree" ("suggestion_id");



CREATE INDEX "idx_planning_invitees_accepted" ON "public"."planning_invitees" USING "btree" ("session_id", "accepted_at") WHERE ("accepted_at" IS NOT NULL);



CREATE INDEX "idx_planning_invitees_session" ON "public"."planning_invitees" USING "btree" ("session_id");



CREATE INDEX "idx_planning_invitees_slots" ON "public"."planning_invitees" USING "btree" ("session_id") WHERE ("has_slot" = true);



CREATE INDEX "idx_planning_invitees_user" ON "public"."planning_invitees" USING "btree" ("user_id");



CREATE INDEX "idx_planning_session_items_added_by" ON "public"."planning_session_items" USING "btree" ("added_by_user_id") WHERE ("added_by_user_id" IS NOT NULL);



CREATE INDEX "idx_planning_session_items_claimed" ON "public"."planning_session_items" USING "btree" ("claimed_by_user_id") WHERE ("claimed_by_user_id" IS NOT NULL);



CREATE INDEX "idx_planning_session_items_session" ON "public"."planning_session_items" USING "btree" ("session_id");



CREATE INDEX "idx_planning_sessions_creator" ON "public"."planning_sessions" USING "btree" ("created_by_user_id");



CREATE INDEX "idx_planning_sessions_group" ON "public"."planning_sessions" USING "btree" ("group_id");



CREATE INDEX "idx_planning_sessions_status" ON "public"."planning_sessions" USING "btree" ("status", "response_deadline");



CREATE INDEX "idx_player_requests_active" ON "public"."player_requests" USING "btree" ("is_active", "expires_at");



CREATE INDEX "idx_player_requests_event" ON "public"."player_requests" USING "btree" ("event_id");



CREATE INDEX "idx_player_requests_expires" ON "public"."player_requests" USING "btree" ("expires_at") WHERE (("status")::"text" = 'open'::"text");



CREATE INDEX "idx_player_requests_status" ON "public"."player_requests" USING "btree" ("status") WHERE (("status")::"text" = 'open'::"text");



CREATE INDEX "idx_player_requests_user" ON "public"."player_requests" USING "btree" ("user_id");



CREATE INDEX "idx_pokemon_cards_name" ON "public"."pokemon_cards_cache" USING "btree" ("name");



CREATE INDEX "idx_pokemon_cards_name_lower" ON "public"."pokemon_cards_cache" USING "btree" ("lower"(("name")::"text"));



CREATE INDEX "idx_pokemon_cards_search" ON "public"."pokemon_cards_cache" USING "gin" ("search_vector");



CREATE INDEX "idx_pokemon_cards_set_id" ON "public"."pokemon_cards_cache" USING "btree" ("set_id");



CREATE INDEX "idx_pokemon_cards_stale_at" ON "public"."pokemon_cards_cache" USING "btree" ("stale_at");



CREATE INDEX "idx_pokemon_cards_supertype" ON "public"."pokemon_cards_cache" USING "btree" ("supertype");



CREATE INDEX "idx_pokemon_cards_types" ON "public"."pokemon_cards_cache" USING "gin" ("types");



CREATE INDEX "idx_pokemon_deck_cards_deck" ON "public"."pokemon_deck_cards" USING "btree" ("deck_id");



CREATE INDEX "idx_pokemon_deck_cards_tcg_id" ON "public"."pokemon_deck_cards" USING "btree" ("pokemon_tcg_id");



CREATE INDEX "idx_pokemon_decks_archetype" ON "public"."pokemon_decks" USING "btree" ("archetype") WHERE ("archetype" IS NOT NULL);



CREATE INDEX "idx_pokemon_decks_format" ON "public"."pokemon_decks" USING "btree" ("format_id");



CREATE INDEX "idx_pokemon_decks_owner" ON "public"."pokemon_decks" USING "btree" ("owner_user_id");



CREATE INDEX "idx_pokemon_decks_public" ON "public"."pokemon_decks" USING "btree" ("is_public") WHERE ("is_public" = true);



CREATE INDEX "idx_pokemon_event_config_event_id" ON "public"."pokemon_event_config" USING "btree" ("event_id");



CREATE INDEX "idx_pokemon_event_config_format_id" ON "public"."pokemon_event_config" USING "btree" ("format_id");



CREATE INDEX "idx_pokemon_event_reg_deck" ON "public"."pokemon_event_registrations" USING "btree" ("deck_id");



CREATE INDEX "idx_pokemon_event_reg_registration" ON "public"."pokemon_event_registrations" USING "btree" ("registration_id");



CREATE INDEX "idx_pokemon_sets_name" ON "public"."pokemon_sets_cache" USING "btree" ("name");



CREATE INDEX "idx_pokemon_sets_release_date" ON "public"."pokemon_sets_cache" USING "btree" ("release_date" DESC);



CREATE INDEX "idx_pokemon_sets_series" ON "public"."pokemon_sets_cache" USING "btree" ("series");



CREATE INDEX "idx_pokemon_sets_stale_at" ON "public"."pokemon_sets_cache" USING "btree" ("stale_at");



CREATE INDEX "idx_raffle_entries_raffle" ON "public"."raffle_entries" USING "btree" ("raffle_id");



CREATE INDEX "idx_raffle_entries_source" ON "public"."raffle_entries" USING "btree" ("entry_type", "source_id") WHERE ("source_id" IS NOT NULL);



CREATE UNIQUE INDEX "idx_raffle_entries_unique_source" ON "public"."raffle_entries" USING "btree" ("raffle_id", "user_id", "entry_type", "source_id") WHERE ("source_id" IS NOT NULL);



CREATE INDEX "idx_raffle_entries_user" ON "public"."raffle_entries" USING "btree" ("user_id");



CREATE INDEX "idx_raffles_active" ON "public"."raffles" USING "btree" ("status", "start_date", "end_date") WHERE (("status")::"text" = 'active'::"text");



CREATE INDEX "idx_raffles_dates" ON "public"."raffles" USING "btree" ("start_date", "end_date");



CREATE INDEX "idx_raffles_status" ON "public"."raffles" USING "btree" ("status");



CREATE INDEX "idx_recurring_games_group" ON "public"."recurring_games" USING "btree" ("group_id");



CREATE INDEX "idx_scryfall_cached_at" ON "public"."scryfall_cards_cache" USING "btree" ("cached_at");



CREATE INDEX "idx_scryfall_name" ON "public"."scryfall_cards_cache" USING "btree" ("name");



CREATE INDEX "idx_scryfall_name_trgm" ON "public"."scryfall_cards_cache" USING "gin" ("name" "public"."gin_trgm_ops");



CREATE INDEX "idx_scryfall_oracle_id" ON "public"."scryfall_cards_cache" USING "btree" ("oracle_id");



CREATE INDEX "idx_scryfall_set_code" ON "public"."scryfall_cards_cache" USING "btree" ("set_code");



CREATE INDEX "idx_stripe_invoices_date" ON "public"."stripe_invoices" USING "btree" ("user_id", "invoice_date" DESC);



CREATE INDEX "idx_stripe_invoices_user" ON "public"."stripe_invoices" USING "btree" ("user_id");



CREATE INDEX "idx_subscription_events_user" ON "public"."subscription_events" USING "btree" ("user_id", "created_at" DESC);



CREATE INDEX "idx_users_account_status" ON "public"."users" USING "btree" ("account_status") WHERE (("account_status")::"text" <> 'active'::"text");



CREATE INDEX "idx_users_active_event_location" ON "public"."users" USING "btree" ("active_event_location_id") WHERE ("active_event_location_id" IS NOT NULL);



CREATE INDEX "idx_users_active_location" ON "public"."users" USING "btree" ("active_city", "active_state") WHERE ("active_city" IS NOT NULL);



CREATE INDEX "idx_users_blocked" ON "public"."users" USING "gin" ("blocked_user_ids");



CREATE INDEX "idx_users_founding_member" ON "public"."users" USING "btree" ("is_founding_member") WHERE ("is_founding_member" = true);



CREATE INDEX "idx_users_is_suspended" ON "public"."users" USING "btree" ("is_suspended") WHERE ("is_suspended" = true);



CREATE INDEX "idx_users_is_test_account" ON "public"."users" USING "btree" ("is_test_account") WHERE ("is_test_account" = true);



CREATE INDEX "idx_users_location" ON "public"."users" USING "btree" ("home_city", "home_state");



CREATE INDEX "idx_users_password_changed_at" ON "public"."users" USING "btree" ("password_changed_at");



CREATE INDEX "idx_users_stripe_customer" ON "public"."users" USING "btree" ("stripe_customer_id") WHERE ("stripe_customer_id" IS NOT NULL);



CREATE INDEX "idx_users_timezone" ON "public"."users" USING "btree" ("timezone");



CREATE UNIQUE INDEX "idx_users_username_lower" ON "public"."users" USING "btree" ("lower"(("username")::"text"));



CREATE INDEX "idx_zip_codes_city_state" ON "public"."zip_codes" USING "btree" ("city", "state");



CREATE INDEX "idx_zip_codes_coords" ON "public"."zip_codes" USING "btree" ("latitude", "longitude");



CREATE INDEX "idx_zip_codes_state" ON "public"."zip_codes" USING "btree" ("state");



CREATE INDEX "ix_events_city" ON "public"."events" USING "btree" ("city");



CREATE INDEX "ix_events_date" ON "public"."events" USING "btree" ("event_date");



CREATE OR REPLACE TRIGGER "bgg_cache_search_update" BEFORE INSERT OR UPDATE ON "public"."bgg_games_cache" FOR EACH ROW EXECUTE FUNCTION "public"."update_bgg_search_vector"();



CREATE OR REPLACE TRIGGER "deck_cards_count_update" AFTER INSERT OR DELETE OR UPDATE ON "public"."mtg_deck_cards" FOR EACH ROW EXECUTE FUNCTION "public"."update_deck_card_count"();



CREATE OR REPLACE TRIGGER "groups_updated_at" BEFORE UPDATE ON "public"."groups" FOR EACH ROW EXECUTE FUNCTION "public"."update_groups_updated_at"();



CREATE OR REPLACE TRIGGER "mtg_deck_updated" BEFORE UPDATE ON "public"."mtg_decks" FOR EACH ROW EXECUTE FUNCTION "public"."update_mtg_deck_timestamp"();



CREATE OR REPLACE TRIGGER "mtg_event_config_updated" BEFORE UPDATE ON "public"."mtg_event_config" FOR EACH ROW EXECUTE FUNCTION "public"."update_mtg_event_config_timestamp"();



CREATE OR REPLACE TRIGGER "mtg_event_registration_updated" BEFORE UPDATE ON "public"."mtg_event_registrations" FOR EACH ROW EXECUTE FUNCTION "public"."update_mtg_event_registration_timestamp"();



CREATE OR REPLACE TRIGGER "pokemon_cards_search_vector_trigger" BEFORE INSERT OR UPDATE ON "public"."pokemon_cards_cache" FOR EACH ROW EXECUTE FUNCTION "public"."pokemon_cards_search_vector_update"();



CREATE OR REPLACE TRIGGER "pokemon_deck_cards_count_update" AFTER INSERT OR DELETE OR UPDATE ON "public"."pokemon_deck_cards" FOR EACH ROW EXECUTE FUNCTION "public"."update_pokemon_deck_counts"();



CREATE OR REPLACE TRIGGER "pokemon_deck_updated" BEFORE UPDATE ON "public"."pokemon_decks" FOR EACH ROW EXECUTE FUNCTION "public"."update_pokemon_deck_timestamp"();



CREATE OR REPLACE TRIGGER "pokemon_event_config_updated" BEFORE UPDATE ON "public"."pokemon_event_config" FOR EACH ROW EXECUTE FUNCTION "public"."update_pokemon_event_config_timestamp"();



CREATE OR REPLACE TRIGGER "pokemon_event_registration_updated" BEFORE UPDATE ON "public"."pokemon_event_registrations" FOR EACH ROW EXECUTE FUNCTION "public"."update_pokemon_event_registration_timestamp"();



ALTER TABLE ONLY "public"."ad_clicks"
    ADD CONSTRAINT "ad_clicks_ad_id_fkey" FOREIGN KEY ("ad_id") REFERENCES "public"."ads"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."ad_clicks"
    ADD CONSTRAINT "ad_clicks_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."ad_impressions"
    ADD CONSTRAINT "ad_impressions_ad_id_fkey" FOREIGN KEY ("ad_id") REFERENCES "public"."ads"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."ad_impressions"
    ADD CONSTRAINT "ad_impressions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."admin_bugs"
    ADD CONSTRAINT "admin_bugs_assigned_to_user_id_fkey" FOREIGN KEY ("assigned_to_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."admin_bugs"
    ADD CONSTRAINT "admin_bugs_reported_by_user_id_fkey" FOREIGN KEY ("reported_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."admin_notes"
    ADD CONSTRAINT "admin_notes_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."chat_messages"
    ADD CONSTRAINT "chat_messages_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."chat_moderation_actions"
    ADD CONSTRAINT "chat_moderation_actions_issued_by_fkey" FOREIGN KEY ("issued_by") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."chat_moderation_actions"
    ADD CONSTRAINT "chat_moderation_actions_report_id_fkey" FOREIGN KEY ("report_id") REFERENCES "public"."chat_reports"("id");



ALTER TABLE ONLY "public"."chat_moderation_actions"
    ADD CONSTRAINT "chat_moderation_actions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."chat_reports"
    ADD CONSTRAINT "chat_reports_message_id_fkey" FOREIGN KEY ("message_id") REFERENCES "public"."chat_messages"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."chat_reports"
    ADD CONSTRAINT "chat_reports_reporter_id_fkey" FOREIGN KEY ("reporter_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."chat_reports"
    ADD CONSTRAINT "chat_reports_reviewed_by_fkey" FOREIGN KEY ("reviewed_by") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."contact_submissions"
    ADD CONSTRAINT "contact_submissions_replied_by_fkey" FOREIGN KEY ("replied_by") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."contact_submissions"
    ADD CONSTRAINT "contact_submissions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."event_game_sessions"
    ADD CONSTRAINT "event_game_sessions_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_game_sessions"
    ADD CONSTRAINT "event_game_sessions_table_id_fkey" FOREIGN KEY ("table_id") REFERENCES "public"."event_tables"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_games"
    ADD CONSTRAINT "event_games_added_by_user_id_fkey" FOREIGN KEY ("added_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."event_games"
    ADD CONSTRAINT "event_games_bgg_id_fkey" FOREIGN KEY ("bgg_id") REFERENCES "public"."bgg_games_cache"("bgg_id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."event_games"
    ADD CONSTRAINT "event_games_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_items"
    ADD CONSTRAINT "event_items_claimed_by_user_id_fkey" FOREIGN KEY ("claimed_by_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."event_items"
    ADD CONSTRAINT "event_items_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_locations"
    ADD CONSTRAINT "event_locations_approved_by_user_id_fkey" FOREIGN KEY ("approved_by_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."event_locations"
    ADD CONSTRAINT "event_locations_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."event_registrations"
    ADD CONSTRAINT "event_registrations_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_registrations"
    ADD CONSTRAINT "event_registrations_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."event_tables"
    ADD CONSTRAINT "event_tables_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."events"
    ADD CONSTRAINT "events_event_location_id_fkey" FOREIGN KEY ("event_location_id") REFERENCES "public"."event_locations"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."events"
    ADD CONSTRAINT "events_from_planning_session_id_fkey" FOREIGN KEY ("from_planning_session_id") REFERENCES "public"."planning_sessions"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."events"
    ADD CONSTRAINT "events_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."events"
    ADD CONSTRAINT "events_host_user_id_fkey" FOREIGN KEY ("host_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_sessions"
    ADD CONSTRAINT "fk_finalized_game" FOREIGN KEY ("finalized_game_id") REFERENCES "public"."planning_game_suggestions"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."game_invitations"
    ADD CONSTRAINT "game_invitations_accepted_by_user_id_fkey" FOREIGN KEY ("accepted_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."game_invitations"
    ADD CONSTRAINT "game_invitations_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."game_invitations"
    ADD CONSTRAINT "game_invitations_invited_by_user_id_fkey" FOREIGN KEY ("invited_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."game_session_registrations"
    ADD CONSTRAINT "game_session_registrations_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."event_game_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."game_session_registrations"
    ADD CONSTRAINT "game_session_registrations_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_invitation_uses"
    ADD CONSTRAINT "group_invitation_uses_invitation_id_fkey" FOREIGN KEY ("invitation_id") REFERENCES "public"."group_invitations"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_invitation_uses"
    ADD CONSTRAINT "group_invitation_uses_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_invitations"
    ADD CONSTRAINT "group_invitations_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_invitations"
    ADD CONSTRAINT "group_invitations_invited_by_user_id_fkey" FOREIGN KEY ("invited_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."group_invitations"
    ADD CONSTRAINT "group_invitations_invited_user_id_fkey" FOREIGN KEY ("invited_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_join_requests"
    ADD CONSTRAINT "group_join_requests_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_join_requests"
    ADD CONSTRAINT "group_join_requests_reviewed_by_user_id_fkey" FOREIGN KEY ("reviewed_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."group_join_requests"
    ADD CONSTRAINT "group_join_requests_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_memberships"
    ADD CONSTRAINT "group_memberships_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."group_memberships"
    ADD CONSTRAINT "group_memberships_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."groups"
    ADD CONSTRAINT "groups_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."mtg_deck_cards"
    ADD CONSTRAINT "mtg_deck_cards_deck_id_fkey" FOREIGN KEY ("deck_id") REFERENCES "public"."mtg_decks"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."mtg_decks"
    ADD CONSTRAINT "mtg_decks_format_id_fkey" FOREIGN KEY ("format_id") REFERENCES "public"."mtg_formats"("id");



ALTER TABLE ONLY "public"."mtg_decks"
    ADD CONSTRAINT "mtg_decks_owner_user_id_fkey" FOREIGN KEY ("owner_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."mtg_event_config"
    ADD CONSTRAINT "mtg_event_config_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."mtg_event_config"
    ADD CONSTRAINT "mtg_event_config_format_id_fkey" FOREIGN KEY ("format_id") REFERENCES "public"."mtg_formats"("id");



ALTER TABLE ONLY "public"."mtg_event_registrations"
    ADD CONSTRAINT "mtg_event_registrations_deck_id_fkey" FOREIGN KEY ("deck_id") REFERENCES "public"."mtg_decks"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."mtg_event_registrations"
    ADD CONSTRAINT "mtg_event_registrations_registration_id_fkey" FOREIGN KEY ("registration_id") REFERENCES "public"."event_registrations"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_date_votes"
    ADD CONSTRAINT "planning_date_votes_date_id_fkey" FOREIGN KEY ("date_id") REFERENCES "public"."planning_dates"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_date_votes"
    ADD CONSTRAINT "planning_date_votes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_dates"
    ADD CONSTRAINT "planning_dates_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."planning_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_game_suggestions"
    ADD CONSTRAINT "planning_game_suggestions_bgg_id_fkey" FOREIGN KEY ("bgg_id") REFERENCES "public"."bgg_games_cache"("bgg_id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."planning_game_suggestions"
    ADD CONSTRAINT "planning_game_suggestions_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."planning_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_game_suggestions"
    ADD CONSTRAINT "planning_game_suggestions_suggested_by_user_id_fkey" FOREIGN KEY ("suggested_by_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_game_votes"
    ADD CONSTRAINT "planning_game_votes_suggestion_id_fkey" FOREIGN KEY ("suggestion_id") REFERENCES "public"."planning_game_suggestions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_game_votes"
    ADD CONSTRAINT "planning_game_votes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_invitees"
    ADD CONSTRAINT "planning_invitees_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."planning_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_invitees"
    ADD CONSTRAINT "planning_invitees_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_session_items"
    ADD CONSTRAINT "planning_session_items_added_by_user_id_fkey" FOREIGN KEY ("added_by_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."planning_session_items"
    ADD CONSTRAINT "planning_session_items_claimed_by_user_id_fkey" FOREIGN KEY ("claimed_by_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."planning_session_items"
    ADD CONSTRAINT "planning_session_items_session_id_fkey" FOREIGN KEY ("session_id") REFERENCES "public"."planning_sessions"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_sessions"
    ADD CONSTRAINT "planning_sessions_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."planning_sessions"
    ADD CONSTRAINT "planning_sessions_created_event_id_fkey" FOREIGN KEY ("created_event_id") REFERENCES "public"."events"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."planning_sessions"
    ADD CONSTRAINT "planning_sessions_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."player_requests"
    ADD CONSTRAINT "player_requests_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."player_requests"
    ADD CONSTRAINT "player_requests_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pokemon_deck_cards"
    ADD CONSTRAINT "pokemon_deck_cards_deck_id_fkey" FOREIGN KEY ("deck_id") REFERENCES "public"."pokemon_decks"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pokemon_decks"
    ADD CONSTRAINT "pokemon_decks_format_id_fkey" FOREIGN KEY ("format_id") REFERENCES "public"."pokemon_formats"("id");



ALTER TABLE ONLY "public"."pokemon_decks"
    ADD CONSTRAINT "pokemon_decks_owner_user_id_fkey" FOREIGN KEY ("owner_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pokemon_event_config"
    ADD CONSTRAINT "pokemon_event_config_event_id_fkey" FOREIGN KEY ("event_id") REFERENCES "public"."events"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."pokemon_event_config"
    ADD CONSTRAINT "pokemon_event_config_format_id_fkey" FOREIGN KEY ("format_id") REFERENCES "public"."pokemon_formats"("id");



ALTER TABLE ONLY "public"."pokemon_event_registrations"
    ADD CONSTRAINT "pokemon_event_registrations_deck_id_fkey" FOREIGN KEY ("deck_id") REFERENCES "public"."pokemon_decks"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."pokemon_event_registrations"
    ADD CONSTRAINT "pokemon_event_registrations_registration_id_fkey" FOREIGN KEY ("registration_id") REFERENCES "public"."event_registrations"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."raffle_entries"
    ADD CONSTRAINT "raffle_entries_raffle_id_fkey" FOREIGN KEY ("raffle_id") REFERENCES "public"."raffles"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."raffle_entries"
    ADD CONSTRAINT "raffle_entries_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."raffles"
    ADD CONSTRAINT "raffles_created_by_user_id_fkey" FOREIGN KEY ("created_by_user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."raffles"
    ADD CONSTRAINT "raffles_winner_user_id_fkey" FOREIGN KEY ("winner_user_id") REFERENCES "public"."users"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."recurring_games"
    ADD CONSTRAINT "recurring_games_group_id_fkey" FOREIGN KEY ("group_id") REFERENCES "public"."groups"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."stripe_invoices"
    ADD CONSTRAINT "stripe_invoices_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."subscription_events"
    ADD CONSTRAINT "subscription_events_admin_user_id_fkey" FOREIGN KEY ("admin_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."subscription_events"
    ADD CONSTRAINT "subscription_events_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users"("id") ON DELETE CASCADE;



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_active_event_location_id_fkey" FOREIGN KEY ("active_event_location_id") REFERENCES "public"."event_locations"("id") ON DELETE SET NULL;



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_banned_by_user_id_fkey" FOREIGN KEY ("banned_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_subscription_override_by_user_id_fkey" FOREIGN KEY ("subscription_override_by_user_id") REFERENCES "public"."users"("id");



ALTER TABLE ONLY "public"."users"
    ADD CONSTRAINT "users_suspended_by_user_id_fkey" FOREIGN KEY ("suspended_by_user_id") REFERENCES "public"."users"("id");



CREATE POLICY "Active requests viewable by all" ON "public"."player_requests" FOR SELECT USING ((("is_active" = true) AND (("expires_at" IS NULL) OR ("expires_at" > "now"()))));



CREATE POLICY "Admins can delete locations" ON "public"."event_locations" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE ((("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")) AND ("users"."is_admin" = true)))));



CREATE POLICY "Admins can do everything with bugs" ON "public"."admin_bugs" USING ("public"."is_current_user_admin"()) WITH CHECK ("public"."is_current_user_admin"());



CREATE POLICY "Admins can do everything with notes" ON "public"."admin_notes" USING ("public"."is_current_user_admin"()) WITH CHECK ("public"."is_current_user_admin"());



CREATE POLICY "Admins can manage all ads" ON "public"."ads" USING ("public"."is_current_user_admin"()) WITH CHECK ("public"."is_current_user_admin"());



CREATE POLICY "Admins can manage zip codes" ON "public"."zip_codes" USING ("public"."is_current_user_admin"()) WITH CHECK ("public"."is_current_user_admin"());



CREATE POLICY "Admins can update locations" ON "public"."event_locations" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE ((("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")) AND ("users"."is_admin" = true)))));



CREATE POLICY "Admins can view all locations" ON "public"."event_locations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE ((("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")) AND ("users"."is_admin" = true)))));



CREATE POLICY "Admins can view clicks" ON "public"."ad_clicks" FOR SELECT USING ("public"."is_current_user_admin"());



CREATE POLICY "Admins can view impressions" ON "public"."ad_impressions" FOR SELECT USING ("public"."is_current_user_admin"());



CREATE POLICY "All groups viewable by everyone" ON "public"."groups" FOR SELECT USING (true);



CREATE POLICY "Anyone can read zip codes" ON "public"."zip_codes" FOR SELECT USING (true);



CREATE POLICY "Anyone can record clicks" ON "public"."ad_clicks" FOR INSERT WITH CHECK (true);



CREATE POLICY "Anyone can record impressions" ON "public"."ad_impressions" FOR INSERT WITH CHECK (true);



CREATE POLICY "Anyone can view active ads" ON "public"."ads" FOR SELECT USING (("is_active" = true));



CREATE POLICY "Approved active locations viewable by all" ON "public"."event_locations" FOR SELECT USING (((("status")::"text" = 'approved'::"text") AND (("is_permanent" = true) OR (("end_date" IS NOT NULL) AND ("end_date" >= CURRENT_DATE)) OR (("start_date" IS NULL) AND ("end_date" IS NULL) AND ("is_permanent" = false)))));



CREATE POLICY "Authenticated users can create locations" ON "public"."event_locations" FOR INSERT WITH CHECK (("created_by_user_id" IN ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))));



CREATE POLICY "BGG cache readable by everyone" ON "public"."bgg_games_cache" FOR SELECT USING (true);



CREATE POLICY "Creators can manage their sessions" ON "public"."planning_sessions" USING (("created_by_user_id" = "public"."get_current_user_id"())) WITH CHECK (("created_by_user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Event games readable for public events" ON "public"."event_games" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_games"."event_id") AND ("events"."is_public" = true)))));



CREATE POLICY "Event games readable for registered users" ON "public"."event_games" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."event_registrations" "er"
     JOIN "public"."users" "u" ON (("er"."user_id" = "u"."id")))
  WHERE (("er"."event_id" = "event_games"."event_id") AND (("u"."firebase_uid")::"text" = ("auth"."uid"())::"text")))));



CREATE POLICY "Event items viewable for public events" ON "public"."event_items" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_items"."event_id") AND ("events"."is_public" = true) AND (("events"."status")::"text" = 'published'::"text")))));



CREATE POLICY "Group admins can manage invitations" ON "public"."group_invitations" USING ((EXISTS ( SELECT 1
   FROM "public"."group_memberships"
  WHERE (("group_memberships"."group_id" = "group_invitations"."group_id") AND ("group_memberships"."user_id" = "public"."get_current_user_id"()) AND (("group_memberships"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[])))))) WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."group_memberships"
  WHERE (("group_memberships"."group_id" = "group_invitations"."group_id") AND ("group_memberships"."user_id" = "public"."get_current_user_id"()) AND (("group_memberships"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "Group admins can update requests for their groups" ON "public"."group_join_requests" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."group_memberships"
  WHERE (("group_memberships"."group_id" = "group_join_requests"."group_id") AND ("group_memberships"."user_id" = "public"."get_current_user_id"()) AND (("group_memberships"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "Group admins can view invitation uses" ON "public"."group_invitation_uses" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."group_invitations" "gi"
     JOIN "public"."group_memberships" "gm" ON (("gm"."group_id" = "gi"."group_id")))
  WHERE (("gi"."id" = "group_invitation_uses"."invitation_id") AND ("gm"."user_id" = "public"."get_current_user_id"()) AND (("gm"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "Group admins can view requests for their groups" ON "public"."group_join_requests" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."group_memberships"
  WHERE (("group_memberships"."group_id" = "group_join_requests"."group_id") AND ("group_memberships"."user_id" = "public"."get_current_user_id"()) AND (("group_memberships"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "Group members can view dates for group sessions" ON "public"."planning_dates" FOR SELECT USING ("public"."is_user_in_session_group"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Group members can view their group's sessions" ON "public"."planning_sessions" FOR SELECT USING ((("group_id" IS NOT NULL) AND (EXISTS ( SELECT 1
   FROM "public"."group_memberships"
  WHERE (("group_memberships"."group_id" = "planning_sessions"."group_id") AND ("group_memberships"."user_id" = "public"."get_current_user_id"()))))));



CREATE POLICY "Hosts can manage invitations" ON "public"."game_invitations" USING ((("invited_by_user_id" = ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))) OR ("event_id" IN ( SELECT "events"."id"
   FROM "public"."events"
  WHERE ("events"."host_user_id" = ( SELECT "users"."id"
           FROM "public"."users"
          WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text"))))))));



CREATE POLICY "Invitations viewable by code" ON "public"."game_invitations" FOR SELECT USING (true);



CREATE POLICY "Invited users can view their invitations" ON "public"."group_invitations" FOR SELECT USING (("invited_user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Invitees can add items" ON "public"."planning_session_items" FOR INSERT WITH CHECK ("public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Invitees can add suggestions" ON "public"."planning_game_suggestions" FOR INSERT WITH CHECK ((("suggested_by_user_id" = "public"."get_current_user_id"()) AND "public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"())));



CREATE POLICY "Invitees can view all game votes in their sessions" ON "public"."planning_game_votes" FOR SELECT USING ("public"."is_user_invited_to_session"("public"."get_session_id_from_suggestion"("suggestion_id"), "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view all votes in their sessions" ON "public"."planning_date_votes" FOR SELECT USING ("public"."is_user_invited_to_session"("public"."get_session_id_from_date"("date_id"), "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view dates" ON "public"."planning_dates" FOR SELECT USING ("public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view items in their sessions" ON "public"."planning_session_items" FOR SELECT USING ("public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view other invitees in same session" ON "public"."planning_invitees" FOR SELECT USING ("public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view sessions they're invited to" ON "public"."planning_sessions" FOR SELECT USING ("public"."is_user_invited_to_session"("id", "public"."get_current_user_id"()));



CREATE POLICY "Invitees can view suggestions in their sessions" ON "public"."planning_game_suggestions" FOR SELECT USING ("public"."is_user_invited_to_session"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Memberships viewable by everyone" ON "public"."group_memberships" FOR SELECT USING (true);



CREATE POLICY "Public events are viewable by everyone" ON "public"."events" FOR SELECT USING ((("is_public" = true) AND (("status")::"text" = 'published'::"text")));



CREATE POLICY "Recurring games viewable by everyone" ON "public"."recurring_games" FOR SELECT USING (true);



CREATE POLICY "Registrations viewable for public events" ON "public"."event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_registrations"."event_id") AND ("events"."is_public" = true) AND (("events"."status")::"text" = 'published'::"text")))));



CREATE POLICY "Session creators can manage all items" ON "public"."planning_session_items" USING ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"())) WITH CHECK ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Session creators can manage all suggestions" ON "public"."planning_game_suggestions" USING ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"())) WITH CHECK ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Session creators can manage dates" ON "public"."planning_dates" USING ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"())) WITH CHECK ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Session creators can manage invitees" ON "public"."planning_invitees" USING ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"())) WITH CHECK ("public"."is_user_session_creator"("session_id", "public"."get_current_user_id"()));



CREATE POLICY "Session creators can view all game votes" ON "public"."planning_game_votes" FOR SELECT USING ("public"."is_user_session_creator"("public"."get_session_id_from_suggestion"("suggestion_id"), "public"."get_current_user_id"()));



CREATE POLICY "Session creators can view all votes" ON "public"."planning_date_votes" FOR SELECT USING ("public"."is_user_session_creator"("public"."get_session_id_from_date"("date_id"), "public"."get_current_user_id"()));



CREATE POLICY "Users can create their own join requests" ON "public"."group_join_requests" FOR INSERT WITH CHECK (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can delete items they added" ON "public"."planning_session_items" FOR DELETE USING (("added_by_user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can delete their own pending requests" ON "public"."group_join_requests" FOR DELETE USING ((("user_id" = "public"."get_current_user_id"()) AND (("status")::"text" = 'pending'::"text")));



CREATE POLICY "Users can delete their own suggestions" ON "public"."planning_game_suggestions" FOR DELETE USING (("suggested_by_user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can manage own requests" ON "public"."player_requests" USING (("user_id" = ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))));



CREATE POLICY "Users can manage their own game votes" ON "public"."planning_game_votes" USING (("user_id" = "public"."get_current_user_id"())) WITH CHECK (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can manage their own votes" ON "public"."planning_date_votes" USING (("user_id" = "public"."get_current_user_id"())) WITH CHECK (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can record their own invitation use" ON "public"."group_invitation_uses" FOR INSERT WITH CHECK (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can update items they claimed" ON "public"."planning_session_items" FOR UPDATE USING (("claimed_by_user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can update own profile" ON "public"."users" FOR UPDATE USING ((("firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")));



CREATE POLICY "Users can view and update their own invitee record" ON "public"."planning_invitees" USING (("user_id" = "public"."get_current_user_id"())) WITH CHECK (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users can view own invoices" ON "public"."stripe_invoices" FOR SELECT USING (("user_id" = ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))));



CREATE POLICY "Users can view own location submissions" ON "public"."event_locations" FOR SELECT USING (("created_by_user_id" IN ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))));



CREATE POLICY "Users can view own subscription events" ON "public"."subscription_events" FOR SELECT USING (("user_id" = ( SELECT "users"."id"
   FROM "public"."users"
  WHERE (("users"."firebase_uid")::"text" = (("current_setting"('request.jwt.claims'::"text", true))::json ->> 'sub'::"text")))));



CREATE POLICY "Users can view public profiles" ON "public"."users" FOR SELECT USING (true);



CREATE POLICY "Users can view their own join requests" ON "public"."group_join_requests" FOR SELECT USING (("user_id" = "public"."get_current_user_id"()));



CREATE POLICY "Users viewable as event hosts" ON "public"."users" FOR SELECT USING (true);



ALTER TABLE "public"."ad_clicks" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."ad_impressions" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."admin_bugs" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."admin_notes" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."ads" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."bgg_games_cache" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."chat_messages" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "chat_messages_insert" ON "public"."chat_messages" FOR INSERT WITH CHECK ((("user_id" = "public"."get_current_user_id"()) AND "public"."can_access_chat"("context_type", "context_id", "public"."get_current_user_id"())));



CREATE POLICY "chat_messages_select" ON "public"."chat_messages" FOR SELECT USING (("public"."can_access_chat"("context_type", "context_id", "public"."get_current_user_id"()) AND ("is_deleted" = false)));



CREATE POLICY "chat_messages_update_own" ON "public"."chat_messages" FOR UPDATE USING (("user_id" = "public"."get_current_user_id"())) WITH CHECK (("user_id" = "public"."get_current_user_id"()));



ALTER TABLE "public"."chat_moderation_actions" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "chat_moderation_insert_admin" ON "public"."chat_moderation_actions" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



CREATE POLICY "chat_moderation_select_admin" ON "public"."chat_moderation_actions" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



CREATE POLICY "chat_moderation_select_own" ON "public"."chat_moderation_actions" FOR SELECT USING (("user_id" = "public"."get_current_user_id"()));



ALTER TABLE "public"."chat_reports" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "chat_reports_insert" ON "public"."chat_reports" FOR INSERT WITH CHECK (("reporter_id" = "public"."get_current_user_id"()));



CREATE POLICY "chat_reports_select_admin" ON "public"."chat_reports" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



CREATE POLICY "chat_reports_select_own" ON "public"."chat_reports" FOR SELECT USING (("reporter_id" = "public"."get_current_user_id"()));



CREATE POLICY "chat_reports_update_admin" ON "public"."chat_reports" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



ALTER TABLE "public"."contact_submissions" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."event_game_sessions" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "event_game_sessions_delete" ON "public"."event_game_sessions" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_game_sessions"."event_id") AND ("events"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "event_game_sessions_insert" ON "public"."event_game_sessions" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_game_sessions"."event_id") AND ("events"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "event_game_sessions_select" ON "public"."event_game_sessions" FOR SELECT USING (true);



CREATE POLICY "event_game_sessions_update" ON "public"."event_game_sessions" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_game_sessions"."event_id") AND ("events"."host_user_id" = "auth"."uid"())))));



ALTER TABLE "public"."event_games" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."event_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."event_locations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."event_registrations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."event_tables" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "event_tables_delete" ON "public"."event_tables" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_tables"."event_id") AND ("events"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "event_tables_insert" ON "public"."event_tables" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."events"
  WHERE (("events"."id" = "event_tables"."event_id") AND ("events"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "event_tables_select" ON "public"."event_tables" FOR SELECT USING (true);



ALTER TABLE "public"."events" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."game_invitations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."game_session_registrations" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "game_session_registrations_delete" ON "public"."game_session_registrations" FOR DELETE USING ((("user_id" = "auth"."uid"()) OR (EXISTS ( SELECT 1
   FROM ("public"."event_game_sessions" "egs"
     JOIN "public"."events" "e" ON (("e"."id" = "egs"."event_id")))
  WHERE (("egs"."id" = "game_session_registrations"."session_id") AND ("e"."host_user_id" = "auth"."uid"()))))));



CREATE POLICY "game_session_registrations_insert" ON "public"."game_session_registrations" FOR INSERT WITH CHECK ((("user_id" = "auth"."uid"()) OR (EXISTS ( SELECT 1
   FROM ("public"."event_game_sessions" "egs"
     JOIN "public"."events" "e" ON (("e"."id" = "egs"."event_id")))
  WHERE (("egs"."id" = "game_session_registrations"."session_id") AND ("e"."host_user_id" = "auth"."uid"()))))));



CREATE POLICY "game_session_registrations_select" ON "public"."game_session_registrations" FOR SELECT USING (true);



ALTER TABLE "public"."group_invitation_uses" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."group_invitations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."group_join_requests" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."group_memberships" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."groups" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."mtg_deck_cards" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "mtg_deck_cards_owner_all" ON "public"."mtg_deck_cards" USING ((EXISTS ( SELECT 1
   FROM "public"."mtg_decks" "d"
  WHERE (("d"."id" = "mtg_deck_cards"."deck_id") AND ("d"."owner_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_deck_cards_select_public" ON "public"."mtg_deck_cards" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."mtg_decks" "d"
  WHERE (("d"."id" = "mtg_deck_cards"."deck_id") AND ("d"."is_public" = true)))));



ALTER TABLE "public"."mtg_decks" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "mtg_decks_owner_all" ON "public"."mtg_decks" USING (("owner_user_id" = "auth"."uid"()));



CREATE POLICY "mtg_decks_select_public" ON "public"."mtg_decks" FOR SELECT USING (("is_public" = true));



ALTER TABLE "public"."mtg_event_config" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "mtg_event_config_delete_host" ON "public"."mtg_event_config" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_config_insert_host" ON "public"."mtg_event_config" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_config_manage_group_admin" ON "public"."mtg_event_config" USING ((EXISTS ( SELECT 1
   FROM ("public"."events" "e"
     JOIN "public"."group_memberships" "gm" ON (("gm"."group_id" = "e"."group_id")))
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("gm"."user_id" = "auth"."uid"()) AND (("gm"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "mtg_event_config_select_host" ON "public"."mtg_event_config" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_config_select_public" ON "public"."mtg_event_config" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("e"."is_public" = true)))));



CREATE POLICY "mtg_event_config_update_host" ON "public"."mtg_event_config" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "mtg_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_reg_insert_own" ON "public"."mtg_event_registrations" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "mtg_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_reg_select_host" ON "public"."mtg_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."event_registrations" "er"
     JOIN "public"."events" "e" ON (("e"."id" = "er"."event_id")))
  WHERE (("er"."id" = "mtg_event_registrations"."registration_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_reg_select_own" ON "public"."mtg_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "mtg_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



CREATE POLICY "mtg_event_reg_select_public" ON "public"."mtg_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."event_registrations" "er"
     JOIN "public"."events" "e" ON (("e"."id" = "er"."event_id")))
  WHERE (("er"."id" = "mtg_event_registrations"."registration_id") AND ("e"."is_public" = true)))));



CREATE POLICY "mtg_event_reg_update_own" ON "public"."mtg_event_registrations" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "mtg_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



ALTER TABLE "public"."mtg_event_registrations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."mtg_formats" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "mtg_formats_select" ON "public"."mtg_formats" FOR SELECT USING (true);



ALTER TABLE "public"."planning_date_votes" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_dates" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_game_suggestions" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_game_votes" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_invitees" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_session_items" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."planning_sessions" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."player_requests" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."pokemon_deck_cards" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pokemon_deck_cards_owner_all" ON "public"."pokemon_deck_cards" USING ((EXISTS ( SELECT 1
   FROM "public"."pokemon_decks" "d"
  WHERE (("d"."id" = "pokemon_deck_cards"."deck_id") AND ("d"."owner_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_deck_cards_select_public" ON "public"."pokemon_deck_cards" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."pokemon_decks" "d"
  WHERE (("d"."id" = "pokemon_deck_cards"."deck_id") AND ("d"."is_public" = true)))));



ALTER TABLE "public"."pokemon_decks" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pokemon_decks_owner_all" ON "public"."pokemon_decks" USING (("owner_user_id" = "auth"."uid"()));



CREATE POLICY "pokemon_decks_select_public" ON "public"."pokemon_decks" FOR SELECT USING (("is_public" = true));



ALTER TABLE "public"."pokemon_event_config" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pokemon_event_config_delete_host" ON "public"."pokemon_event_config" FOR DELETE USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_config_insert_host" ON "public"."pokemon_event_config" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_config_manage_group_admin" ON "public"."pokemon_event_config" USING ((EXISTS ( SELECT 1
   FROM ("public"."events" "e"
     JOIN "public"."group_memberships" "gm" ON (("gm"."group_id" = "e"."group_id")))
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("gm"."user_id" = "auth"."uid"()) AND (("gm"."role")::"text" = ANY ((ARRAY['owner'::character varying, 'admin'::character varying])::"text"[]))))));



CREATE POLICY "pokemon_event_config_select_host" ON "public"."pokemon_event_config" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_config_select_public" ON "public"."pokemon_event_config" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("e"."is_public" = true)))));



CREATE POLICY "pokemon_event_config_update_host" ON "public"."pokemon_event_config" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."events" "e"
  WHERE (("e"."id" = "pokemon_event_config"."event_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_reg_insert_own" ON "public"."pokemon_event_registrations" FOR INSERT WITH CHECK ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "pokemon_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_reg_select_host" ON "public"."pokemon_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."event_registrations" "er"
     JOIN "public"."events" "e" ON (("e"."id" = "er"."event_id")))
  WHERE (("er"."id" = "pokemon_event_registrations"."registration_id") AND ("e"."host_user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_reg_select_own" ON "public"."pokemon_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "pokemon_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



CREATE POLICY "pokemon_event_reg_select_public" ON "public"."pokemon_event_registrations" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM ("public"."event_registrations" "er"
     JOIN "public"."events" "e" ON (("e"."id" = "er"."event_id")))
  WHERE (("er"."id" = "pokemon_event_registrations"."registration_id") AND ("e"."is_public" = true)))));



CREATE POLICY "pokemon_event_reg_update_own" ON "public"."pokemon_event_registrations" FOR UPDATE USING ((EXISTS ( SELECT 1
   FROM "public"."event_registrations" "er"
  WHERE (("er"."id" = "pokemon_event_registrations"."registration_id") AND ("er"."user_id" = "auth"."uid"())))));



ALTER TABLE "public"."pokemon_event_registrations" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."pokemon_formats" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "pokemon_formats_select" ON "public"."pokemon_formats" FOR SELECT USING (true);



ALTER TABLE "public"."raffle_entries" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "raffle_entries_admin_select" ON "public"."raffle_entries" FOR SELECT USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



CREATE POLICY "raffle_entries_select_own" ON "public"."raffle_entries" FOR SELECT USING (("user_id" = "public"."get_current_user_id"()));



ALTER TABLE "public"."raffles" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "raffles_admin_all" ON "public"."raffles" USING ((EXISTS ( SELECT 1
   FROM "public"."users"
  WHERE (("users"."id" = "public"."get_current_user_id"()) AND ("users"."is_admin" = true)))));



CREATE POLICY "raffles_select_public" ON "public"."raffles" FOR SELECT USING ((("status")::"text" = ANY ((ARRAY['active'::character varying, 'ended'::character varying])::"text"[])));



ALTER TABLE "public"."recurring_games" ENABLE ROW LEVEL SECURITY;


CREATE POLICY "scryfall_cache_insert" ON "public"."scryfall_cards_cache" FOR INSERT WITH CHECK (false);



CREATE POLICY "scryfall_cache_select" ON "public"."scryfall_cards_cache" FOR SELECT USING (true);



CREATE POLICY "scryfall_cache_update" ON "public"."scryfall_cards_cache" FOR UPDATE USING (false);



ALTER TABLE "public"."scryfall_cards_cache" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."stripe_invoices" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."subscription_events" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."users" ENABLE ROW LEVEL SECURITY;


ALTER TABLE "public"."zip_codes" ENABLE ROW LEVEL SECURITY;


GRANT USAGE ON SCHEMA "public" TO "postgres";
GRANT USAGE ON SCHEMA "public" TO "anon";
GRANT USAGE ON SCHEMA "public" TO "authenticated";
GRANT USAGE ON SCHEMA "public" TO "service_role";



GRANT ALL ON TYPE "public"."chat_context_type" TO "authenticated";



GRANT ALL ON TYPE "public"."chat_moderation_action" TO "authenticated";



GRANT ALL ON TYPE "public"."chat_report_reason" TO "authenticated";



GRANT ALL ON TYPE "public"."chat_report_status" TO "authenticated";



GRANT ALL ON FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."award_raffle_entry"("p_user_id" "uuid", "p_entry_type" character varying, "p_source_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) TO "anon";
GRANT ALL ON FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) TO "authenticated";
GRANT ALL ON FUNCTION "public"."calculate_distance_miles"("lat1" numeric, "lon1" numeric, "lat2" numeric, "lon2" numeric) TO "service_role";



GRANT ALL ON FUNCTION "public"."can_access_chat"("ctx_type" "public"."chat_context_type", "ctx_id" "uuid", "user_uuid" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."can_access_chat"("ctx_type" "public"."chat_context_type", "ctx_id" "uuid", "user_uuid" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."can_access_chat"("ctx_type" "public"."chat_context_type", "ctx_id" "uuid", "user_uuid" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."clean_bgg_cache"() TO "anon";
GRANT ALL ON FUNCTION "public"."clean_bgg_cache"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."clean_bgg_cache"() TO "service_role";



GRANT ALL ON FUNCTION "public"."generate_slug"("name" "text") TO "anon";
GRANT ALL ON FUNCTION "public"."generate_slug"("name" "text") TO "authenticated";
GRANT ALL ON FUNCTION "public"."generate_slug"("name" "text") TO "service_role";



GRANT ALL ON FUNCTION "public"."get_active_raffle"() TO "anon";
GRANT ALL ON FUNCTION "public"."get_active_raffle"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_active_raffle"() TO "service_role";



GRANT ALL ON FUNCTION "public"."get_current_user_id"() TO "anon";
GRANT ALL ON FUNCTION "public"."get_current_user_id"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_current_user_id"() TO "service_role";



GRANT ALL ON FUNCTION "public"."get_popular_games"("limit_count" integer) TO "anon";
GRANT ALL ON FUNCTION "public"."get_popular_games"("limit_count" integer) TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_popular_games"("limit_count" integer) TO "service_role";



GRANT ALL ON FUNCTION "public"."get_raffle_entry_multiplier"("user_uuid" "uuid", "entry_type" character varying) TO "anon";
GRANT ALL ON FUNCTION "public"."get_raffle_entry_multiplier"("user_uuid" "uuid", "entry_type" character varying) TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_raffle_entry_multiplier"("user_uuid" "uuid", "entry_type" character varying) TO "service_role";



GRANT ALL ON FUNCTION "public"."get_session_id_from_date"("p_date_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."get_session_id_from_date"("p_date_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_session_id_from_date"("p_date_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."get_session_id_from_suggestion"("p_suggestion_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."get_session_id_from_suggestion"("p_suggestion_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_session_id_from_suggestion"("p_suggestion_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) TO "anon";
GRANT ALL ON FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) TO "authenticated";
GRANT ALL ON FUNCTION "public"."get_zips_within_radius"("center_zip" character varying, "radius_miles" numeric) TO "service_role";



GRANT ALL ON FUNCTION "public"."is_current_user_admin"() TO "anon";
GRANT ALL ON FUNCTION "public"."is_current_user_admin"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_current_user_admin"() TO "service_role";



GRANT ALL ON FUNCTION "public"."is_event_chat_participant"("event_uuid" "uuid", "user_uuid" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_event_chat_participant"("event_uuid" "uuid", "user_uuid" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_event_chat_participant"("event_uuid" "uuid", "user_uuid" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_group_chat_participant"("group_uuid" "uuid", "user_uuid" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_group_chat_participant"("group_uuid" "uuid", "user_uuid" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_group_chat_participant"("group_uuid" "uuid", "user_uuid" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_planning_chat_participant"("session_uuid" "uuid", "user_uuid" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_planning_chat_participant"("session_uuid" "uuid", "user_uuid" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_planning_chat_participant"("session_uuid" "uuid", "user_uuid" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_user_chat_muted"("user_uuid" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_user_chat_muted"("user_uuid" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_user_chat_muted"("user_uuid" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_user_in_session_group"("p_session_id" "uuid", "p_user_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_user_in_session_group"("p_session_id" "uuid", "p_user_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_user_in_session_group"("p_session_id" "uuid", "p_user_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_user_invited_to_session"("p_session_id" "uuid", "p_user_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_user_invited_to_session"("p_session_id" "uuid", "p_user_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_user_invited_to_session"("p_session_id" "uuid", "p_user_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."is_user_session_creator"("p_session_id" "uuid", "p_user_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."is_user_session_creator"("p_session_id" "uuid", "p_user_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."is_user_session_creator"("p_session_id" "uuid", "p_user_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."pokemon_cards_search_vector_update"() TO "anon";
GRANT ALL ON FUNCTION "public"."pokemon_cards_search_vector_update"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."pokemon_cards_search_vector_update"() TO "service_role";



GRANT ALL ON FUNCTION "public"."search_bgg_cache"("search_query" "text", "result_limit" integer) TO "anon";
GRANT ALL ON FUNCTION "public"."search_bgg_cache"("search_query" "text", "result_limit" integer) TO "authenticated";
GRANT ALL ON FUNCTION "public"."search_bgg_cache"("search_query" "text", "result_limit" integer) TO "service_role";



GRANT ALL ON FUNCTION "public"."select_raffle_winner"("p_raffle_id" "uuid") TO "anon";
GRANT ALL ON FUNCTION "public"."select_raffle_winner"("p_raffle_id" "uuid") TO "authenticated";
GRANT ALL ON FUNCTION "public"."select_raffle_winner"("p_raffle_id" "uuid") TO "service_role";



GRANT ALL ON FUNCTION "public"."update_bgg_search_vector"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_bgg_search_vector"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_bgg_search_vector"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_deck_card_count"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_deck_card_count"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_deck_card_count"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_groups_updated_at"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_groups_updated_at"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_groups_updated_at"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_mtg_deck_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_mtg_deck_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_mtg_deck_timestamp"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_mtg_event_config_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_mtg_event_config_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_mtg_event_config_timestamp"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_mtg_event_registration_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_mtg_event_registration_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_mtg_event_registration_timestamp"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_pokemon_deck_counts"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_pokemon_deck_counts"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_pokemon_deck_counts"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_pokemon_deck_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_pokemon_deck_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_pokemon_deck_timestamp"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_pokemon_event_config_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_pokemon_event_config_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_pokemon_event_config_timestamp"() TO "service_role";



GRANT ALL ON FUNCTION "public"."update_pokemon_event_registration_timestamp"() TO "anon";
GRANT ALL ON FUNCTION "public"."update_pokemon_event_registration_timestamp"() TO "authenticated";
GRANT ALL ON FUNCTION "public"."update_pokemon_event_registration_timestamp"() TO "service_role";



GRANT ALL ON TABLE "public"."ad_clicks" TO "anon";
GRANT ALL ON TABLE "public"."ad_clicks" TO "authenticated";
GRANT ALL ON TABLE "public"."ad_clicks" TO "service_role";



GRANT ALL ON TABLE "public"."ad_impressions" TO "anon";
GRANT ALL ON TABLE "public"."ad_impressions" TO "authenticated";
GRANT ALL ON TABLE "public"."ad_impressions" TO "service_role";



GRANT ALL ON TABLE "public"."ads" TO "anon";
GRANT ALL ON TABLE "public"."ads" TO "authenticated";
GRANT ALL ON TABLE "public"."ads" TO "service_role";



GRANT ALL ON TABLE "public"."ad_stats" TO "anon";
GRANT ALL ON TABLE "public"."ad_stats" TO "authenticated";
GRANT ALL ON TABLE "public"."ad_stats" TO "service_role";



GRANT ALL ON TABLE "public"."admin_bugs" TO "anon";
GRANT ALL ON TABLE "public"."admin_bugs" TO "authenticated";
GRANT ALL ON TABLE "public"."admin_bugs" TO "service_role";



GRANT ALL ON TABLE "public"."admin_notes" TO "anon";
GRANT ALL ON TABLE "public"."admin_notes" TO "authenticated";
GRANT ALL ON TABLE "public"."admin_notes" TO "service_role";



GRANT ALL ON TABLE "public"."bgg_games_cache" TO "anon";
GRANT ALL ON TABLE "public"."bgg_games_cache" TO "authenticated";
GRANT ALL ON TABLE "public"."bgg_games_cache" TO "service_role";



GRANT ALL ON TABLE "public"."chat_messages" TO "anon";
GRANT ALL ON TABLE "public"."chat_messages" TO "authenticated";
GRANT ALL ON TABLE "public"."chat_messages" TO "service_role";



GRANT ALL ON TABLE "public"."chat_moderation_actions" TO "anon";
GRANT ALL ON TABLE "public"."chat_moderation_actions" TO "authenticated";
GRANT ALL ON TABLE "public"."chat_moderation_actions" TO "service_role";



GRANT ALL ON TABLE "public"."chat_reports" TO "anon";
GRANT ALL ON TABLE "public"."chat_reports" TO "authenticated";
GRANT ALL ON TABLE "public"."chat_reports" TO "service_role";



GRANT ALL ON TABLE "public"."contact_submissions" TO "anon";
GRANT ALL ON TABLE "public"."contact_submissions" TO "authenticated";
GRANT ALL ON TABLE "public"."contact_submissions" TO "service_role";



GRANT ALL ON TABLE "public"."event_game_sessions" TO "anon";
GRANT ALL ON TABLE "public"."event_game_sessions" TO "authenticated";
GRANT ALL ON TABLE "public"."event_game_sessions" TO "service_role";



GRANT ALL ON TABLE "public"."event_games" TO "anon";
GRANT ALL ON TABLE "public"."event_games" TO "authenticated";
GRANT ALL ON TABLE "public"."event_games" TO "service_role";



GRANT ALL ON TABLE "public"."event_items" TO "anon";
GRANT ALL ON TABLE "public"."event_items" TO "authenticated";
GRANT ALL ON TABLE "public"."event_items" TO "service_role";



GRANT ALL ON TABLE "public"."event_locations" TO "anon";
GRANT ALL ON TABLE "public"."event_locations" TO "authenticated";
GRANT ALL ON TABLE "public"."event_locations" TO "service_role";



GRANT ALL ON TABLE "public"."event_registrations" TO "anon";
GRANT ALL ON TABLE "public"."event_registrations" TO "authenticated";
GRANT ALL ON TABLE "public"."event_registrations" TO "service_role";



GRANT ALL ON TABLE "public"."event_tables" TO "anon";
GRANT ALL ON TABLE "public"."event_tables" TO "authenticated";
GRANT ALL ON TABLE "public"."event_tables" TO "service_role";



GRANT ALL ON TABLE "public"."events" TO "anon";
GRANT ALL ON TABLE "public"."events" TO "authenticated";
GRANT ALL ON TABLE "public"."events" TO "service_role";



GRANT ALL ON TABLE "public"."game_invitations" TO "anon";
GRANT ALL ON TABLE "public"."game_invitations" TO "authenticated";
GRANT ALL ON TABLE "public"."game_invitations" TO "service_role";



GRANT ALL ON TABLE "public"."game_session_registrations" TO "anon";
GRANT ALL ON TABLE "public"."game_session_registrations" TO "authenticated";
GRANT ALL ON TABLE "public"."game_session_registrations" TO "service_role";



GRANT ALL ON TABLE "public"."group_invitation_uses" TO "anon";
GRANT ALL ON TABLE "public"."group_invitation_uses" TO "authenticated";
GRANT ALL ON TABLE "public"."group_invitation_uses" TO "service_role";



GRANT ALL ON TABLE "public"."group_invitations" TO "anon";
GRANT ALL ON TABLE "public"."group_invitations" TO "authenticated";
GRANT ALL ON TABLE "public"."group_invitations" TO "service_role";



GRANT ALL ON TABLE "public"."group_join_requests" TO "anon";
GRANT ALL ON TABLE "public"."group_join_requests" TO "authenticated";
GRANT ALL ON TABLE "public"."group_join_requests" TO "service_role";



GRANT ALL ON TABLE "public"."group_memberships" TO "anon";
GRANT ALL ON TABLE "public"."group_memberships" TO "authenticated";
GRANT ALL ON TABLE "public"."group_memberships" TO "service_role";



GRANT ALL ON TABLE "public"."groups" TO "anon";
GRANT ALL ON TABLE "public"."groups" TO "authenticated";
GRANT ALL ON TABLE "public"."groups" TO "service_role";



GRANT ALL ON TABLE "public"."hot_locations" TO "anon";
GRANT ALL ON TABLE "public"."hot_locations" TO "authenticated";
GRANT ALL ON TABLE "public"."hot_locations" TO "service_role";



GRANT ALL ON TABLE "public"."mtg_deck_cards" TO "anon";
GRANT ALL ON TABLE "public"."mtg_deck_cards" TO "authenticated";
GRANT ALL ON TABLE "public"."mtg_deck_cards" TO "service_role";



GRANT ALL ON TABLE "public"."mtg_decks" TO "anon";
GRANT ALL ON TABLE "public"."mtg_decks" TO "authenticated";
GRANT ALL ON TABLE "public"."mtg_decks" TO "service_role";



GRANT ALL ON TABLE "public"."mtg_event_config" TO "anon";
GRANT ALL ON TABLE "public"."mtg_event_config" TO "authenticated";
GRANT ALL ON TABLE "public"."mtg_event_config" TO "service_role";



GRANT ALL ON TABLE "public"."mtg_event_registrations" TO "anon";
GRANT ALL ON TABLE "public"."mtg_event_registrations" TO "authenticated";
GRANT ALL ON TABLE "public"."mtg_event_registrations" TO "service_role";



GRANT ALL ON TABLE "public"."mtg_formats" TO "anon";
GRANT ALL ON TABLE "public"."mtg_formats" TO "authenticated";
GRANT ALL ON TABLE "public"."mtg_formats" TO "service_role";



GRANT ALL ON TABLE "public"."planning_date_votes" TO "anon";
GRANT ALL ON TABLE "public"."planning_date_votes" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_date_votes" TO "service_role";



GRANT ALL ON TABLE "public"."planning_dates" TO "anon";
GRANT ALL ON TABLE "public"."planning_dates" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_dates" TO "service_role";



GRANT ALL ON TABLE "public"."planning_game_suggestions" TO "anon";
GRANT ALL ON TABLE "public"."planning_game_suggestions" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_game_suggestions" TO "service_role";



GRANT ALL ON TABLE "public"."planning_game_votes" TO "anon";
GRANT ALL ON TABLE "public"."planning_game_votes" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_game_votes" TO "service_role";



GRANT ALL ON TABLE "public"."planning_invitees" TO "anon";
GRANT ALL ON TABLE "public"."planning_invitees" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_invitees" TO "service_role";



GRANT ALL ON TABLE "public"."planning_session_items" TO "anon";
GRANT ALL ON TABLE "public"."planning_session_items" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_session_items" TO "service_role";



GRANT ALL ON TABLE "public"."planning_sessions" TO "anon";
GRANT ALL ON TABLE "public"."planning_sessions" TO "authenticated";
GRANT ALL ON TABLE "public"."planning_sessions" TO "service_role";



GRANT ALL ON TABLE "public"."player_requests" TO "anon";
GRANT ALL ON TABLE "public"."player_requests" TO "authenticated";
GRANT ALL ON TABLE "public"."player_requests" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_cards_cache" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_cards_cache" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_cards_cache" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_deck_cards" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_deck_cards" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_deck_cards" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_decks" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_decks" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_decks" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_event_config" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_event_config" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_event_config" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_event_registrations" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_event_registrations" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_event_registrations" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_formats" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_formats" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_formats" TO "service_role";



GRANT ALL ON TABLE "public"."pokemon_sets_cache" TO "anon";
GRANT ALL ON TABLE "public"."pokemon_sets_cache" TO "authenticated";
GRANT ALL ON TABLE "public"."pokemon_sets_cache" TO "service_role";



GRANT ALL ON TABLE "public"."raffle_entries" TO "anon";
GRANT ALL ON TABLE "public"."raffle_entries" TO "authenticated";
GRANT ALL ON TABLE "public"."raffle_entries" TO "service_role";



GRANT ALL ON TABLE "public"."raffles" TO "anon";
GRANT ALL ON TABLE "public"."raffles" TO "authenticated";
GRANT ALL ON TABLE "public"."raffles" TO "service_role";



GRANT ALL ON TABLE "public"."recurring_games" TO "anon";
GRANT ALL ON TABLE "public"."recurring_games" TO "authenticated";
GRANT ALL ON TABLE "public"."recurring_games" TO "service_role";



GRANT ALL ON TABLE "public"."scryfall_cards_cache" TO "anon";
GRANT ALL ON TABLE "public"."scryfall_cards_cache" TO "authenticated";
GRANT ALL ON TABLE "public"."scryfall_cards_cache" TO "service_role";



GRANT ALL ON TABLE "public"."stripe_invoices" TO "anon";
GRANT ALL ON TABLE "public"."stripe_invoices" TO "authenticated";
GRANT ALL ON TABLE "public"."stripe_invoices" TO "service_role";



GRANT ALL ON TABLE "public"."subscription_events" TO "anon";
GRANT ALL ON TABLE "public"."subscription_events" TO "authenticated";
GRANT ALL ON TABLE "public"."subscription_events" TO "service_role";



GRANT ALL ON TABLE "public"."users" TO "anon";
GRANT ALL ON TABLE "public"."users" TO "authenticated";
GRANT ALL ON TABLE "public"."users" TO "service_role";



GRANT ALL ON TABLE "public"."zip_codes" TO "anon";
GRANT ALL ON TABLE "public"."zip_codes" TO "authenticated";
GRANT ALL ON TABLE "public"."zip_codes" TO "service_role";



ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON SEQUENCES TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON FUNCTIONS TO "service_role";






ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "postgres";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "anon";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "authenticated";
ALTER DEFAULT PRIVILEGES FOR ROLE "postgres" IN SCHEMA "public" GRANT ALL ON TABLES TO "service_role";







