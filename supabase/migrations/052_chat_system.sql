-- Migration: 052_chat_system.sql
-- Purpose: Add real-time chat messaging for events, groups, and planning sessions

-- ============================================================================
-- CHAT CONTEXT TYPE ENUM
-- ============================================================================

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'chat_context_type') THEN
        CREATE TYPE chat_context_type AS ENUM ('event', 'group', 'planning');
    END IF;
END$$;

-- ============================================================================
-- CHAT MESSAGES TABLE
-- ============================================================================

CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    context_type chat_context_type NOT NULL,
    context_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE,

    -- Ensure content is not empty
    CONSTRAINT content_not_empty CHECK (char_length(trim(content)) > 0),
    -- Limit message length (1000 chars for simple text)
    CONSTRAINT content_max_length CHECK (char_length(content) <= 1000)
);

-- Indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_chat_messages_context ON chat_messages(context_type, context_id);
CREATE INDEX IF NOT EXISTS idx_chat_messages_created ON chat_messages(context_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_chat_messages_user ON chat_messages(user_id);

-- ============================================================================
-- HELPER FUNCTIONS FOR RLS
-- ============================================================================

-- Check if user is participant in an event (host or registered)
CREATE OR REPLACE FUNCTION is_event_chat_participant(event_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM events WHERE id = event_uuid AND host_user_id = user_uuid
    UNION
    SELECT 1 FROM event_registrations
    WHERE event_id = event_uuid
    AND user_id = user_uuid
    AND status IN ('pending', 'confirmed')
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Check if user is member of a group
CREATE OR REPLACE FUNCTION is_group_chat_participant(group_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM group_memberships
    WHERE group_id = group_uuid AND user_id = user_uuid
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Check if user is participant in a planning session
CREATE OR REPLACE FUNCTION is_planning_chat_participant(session_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM planning_sessions
    WHERE id = session_uuid AND created_by_user_id = user_uuid
    UNION
    SELECT 1 FROM planning_invitees
    WHERE session_id = session_uuid AND user_id = user_uuid
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- Master function to check chat access
CREATE OR REPLACE FUNCTION can_access_chat(
  ctx_type chat_context_type,
  ctx_id UUID,
  user_uuid UUID
)
RETURNS BOOLEAN AS $$
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
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE chat_messages ENABLE ROW LEVEL SECURITY;

-- Users can view messages in contexts they participate in (non-deleted only)
CREATE POLICY "chat_messages_select" ON chat_messages
    FOR SELECT
    USING (
        can_access_chat(context_type, context_id, get_current_user_id())
        AND is_deleted = FALSE
    );

-- Users can insert messages in contexts they participate in
CREATE POLICY "chat_messages_insert" ON chat_messages
    FOR INSERT
    WITH CHECK (
        user_id = get_current_user_id()
        AND can_access_chat(context_type, context_id, get_current_user_id())
    );

-- Users can update (soft-delete) their own messages
CREATE POLICY "chat_messages_update_own" ON chat_messages
    FOR UPDATE
    USING (user_id = get_current_user_id())
    WITH CHECK (user_id = get_current_user_id());

-- ============================================================================
-- REALTIME PUBLICATION
-- Enable Supabase Realtime for chat_messages
-- ============================================================================

DO $$
BEGIN
  -- Check if publication exists and add the table
  IF EXISTS (
    SELECT 1 FROM pg_publication WHERE pubname = 'supabase_realtime'
  ) THEN
    -- Only add if not already a member
    IF NOT EXISTS (
      SELECT 1 FROM pg_publication_tables
      WHERE pubname = 'supabase_realtime' AND tablename = 'chat_messages'
    ) THEN
      EXECUTE 'ALTER PUBLICATION supabase_realtime ADD TABLE chat_messages';
    END IF;
  END IF;
EXCEPTION
  WHEN OTHERS THEN
    RAISE NOTICE 'Could not add table to publication: %', SQLERRM;
END $$;

-- ============================================================================
-- GRANTS
-- ============================================================================

GRANT SELECT, INSERT, UPDATE ON chat_messages TO authenticated;
GRANT USAGE ON TYPE chat_context_type TO authenticated;
