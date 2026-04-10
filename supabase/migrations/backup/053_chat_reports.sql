-- Chat report reason enum
CREATE TYPE chat_report_reason AS ENUM (
    'harassment',
    'spam',
    'hate_speech',
    'inappropriate',
    'threats',
    'other'
);

-- Chat report status enum
CREATE TYPE chat_report_status AS ENUM (
    'pending',
    'reviewed',
    'action_taken',
    'dismissed'
);

-- Chat moderation action enum
CREATE TYPE chat_moderation_action AS ENUM (
    'warning',
    'mute_1h',
    'mute_24h',
    'mute_7d',
    'ban_chat'
);

-- Chat reports table
CREATE TABLE IF NOT EXISTS chat_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id UUID NOT NULL REFERENCES chat_messages(id) ON DELETE CASCADE,
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason chat_report_reason NOT NULL,
    details TEXT,
    status chat_report_status NOT NULL DEFAULT 'pending',
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    admin_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Prevent duplicate reports from same user on same message
    CONSTRAINT unique_report_per_user UNIQUE (message_id, reporter_id)
);

-- Chat moderation actions table (for tracking warns/bans)
CREATE TABLE IF NOT EXISTS chat_moderation_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action chat_moderation_action NOT NULL,
    reason TEXT NOT NULL,
    report_id UUID REFERENCES chat_reports(id),
    issued_by UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_chat_reports_status ON chat_reports(status);
CREATE INDEX idx_chat_reports_message ON chat_reports(message_id);
CREATE INDEX idx_chat_reports_created ON chat_reports(created_at DESC);
CREATE INDEX idx_chat_moderation_user ON chat_moderation_actions(user_id);
CREATE INDEX idx_chat_moderation_expires ON chat_moderation_actions(expires_at) WHERE expires_at IS NOT NULL;

-- Function to check if user is chat muted/banned
CREATE OR REPLACE FUNCTION is_user_chat_muted(user_uuid UUID)
RETURNS BOOLEAN AS $$
  SELECT EXISTS (
    SELECT 1 FROM chat_moderation_actions
    WHERE user_id = user_uuid
    AND action IN ('mute_1h', 'mute_24h', 'mute_7d', 'ban_chat')
    AND (expires_at IS NULL OR expires_at > NOW())
  )
$$ LANGUAGE sql SECURITY DEFINER STABLE;

-- RLS policies
ALTER TABLE chat_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE chat_moderation_actions ENABLE ROW LEVEL SECURITY;

-- Users can create reports
CREATE POLICY "chat_reports_insert" ON chat_reports FOR INSERT
    WITH CHECK (reporter_id = get_current_user_id());

-- Users can see their own reports
CREATE POLICY "chat_reports_select_own" ON chat_reports FOR SELECT
    USING (reporter_id = get_current_user_id());

-- Admins can see all reports
CREATE POLICY "chat_reports_select_admin" ON chat_reports FOR SELECT
    USING (EXISTS (SELECT 1 FROM users WHERE id = get_current_user_id() AND is_admin = true));

-- Admins can update reports
CREATE POLICY "chat_reports_update_admin" ON chat_reports FOR UPDATE
    USING (EXISTS (SELECT 1 FROM users WHERE id = get_current_user_id() AND is_admin = true));

-- Admins can view moderation actions
CREATE POLICY "chat_moderation_select_admin" ON chat_moderation_actions FOR SELECT
    USING (EXISTS (SELECT 1 FROM users WHERE id = get_current_user_id() AND is_admin = true));

-- Users can see their own moderation actions
CREATE POLICY "chat_moderation_select_own" ON chat_moderation_actions FOR SELECT
    USING (user_id = get_current_user_id());

-- Admins can insert moderation actions
CREATE POLICY "chat_moderation_insert_admin" ON chat_moderation_actions FOR INSERT
    WITH CHECK (EXISTS (SELECT 1 FROM users WHERE id = get_current_user_id() AND is_admin = true));

-- Grants
GRANT SELECT, INSERT ON chat_reports TO authenticated;
GRANT UPDATE ON chat_reports TO authenticated;
GRANT SELECT, INSERT ON chat_moderation_actions TO authenticated;
GRANT USAGE ON TYPE chat_report_reason TO authenticated;
GRANT USAGE ON TYPE chat_report_status TO authenticated;
GRANT USAGE ON TYPE chat_moderation_action TO authenticated;
