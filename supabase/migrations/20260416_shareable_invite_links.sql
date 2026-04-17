-- Shareable invite links: combined group join + session/event landing
CREATE TABLE shareable_invite_links (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
  created_by_user_id UUID NOT NULL REFERENCES users(id),
  invite_code VARCHAR(16) NOT NULL UNIQUE,
  link_type VARCHAR(20) NOT NULL CHECK (link_type IN ('session', 'group_recurring')),
  planning_session_id UUID REFERENCES planning_sessions(id) ON DELETE CASCADE,
  event_id UUID REFERENCES events(id) ON DELETE SET NULL,
  max_uses INTEGER,
  uses_count INTEGER NOT NULL DEFAULT 0,
  is_active BOOLEAN NOT NULL DEFAULT true,
  expires_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT session_link_needs_target CHECK (
    link_type != 'session' OR planning_session_id IS NOT NULL OR event_id IS NOT NULL
  )
);

CREATE INDEX idx_shareable_invite_links_code ON shareable_invite_links(invite_code);
CREATE INDEX idx_shareable_invite_links_group ON shareable_invite_links(group_id);

-- Track who used which link
CREATE TABLE shareable_invite_link_uses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  link_id UUID NOT NULL REFERENCES shareable_invite_links(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(link_id, user_id)
);

-- RLS
ALTER TABLE shareable_invite_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE shareable_invite_link_uses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read active links" ON shareable_invite_links FOR SELECT USING (true);
CREATE POLICY "Anyone can read link uses" ON shareable_invite_link_uses FOR SELECT USING (true);
