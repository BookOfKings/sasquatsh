-- Admin Notes table for project documentation
CREATE TABLE IF NOT EXISTS admin_notes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(50) DEFAULT 'general',
  is_pinned BOOLEAN DEFAULT FALSE,
  created_by_user_id UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Admin Bugs table for bug tracking
CREATE TABLE IF NOT EXISTS admin_bugs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  description TEXT,
  steps_to_reproduce TEXT,
  status VARCHAR(20) DEFAULT 'open' CHECK (status IN ('open', 'in_progress', 'resolved', 'closed', 'wont_fix')),
  priority VARCHAR(20) DEFAULT 'medium' CHECK (priority IN ('low', 'medium', 'high', 'critical')),
  reported_by_user_id UUID NOT NULL REFERENCES users(id),
  assigned_to_user_id UUID REFERENCES users(id),
  resolved_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_admin_notes_category ON admin_notes(category);
CREATE INDEX IF NOT EXISTS idx_admin_notes_pinned ON admin_notes(is_pinned) WHERE is_pinned = TRUE;
CREATE INDEX IF NOT EXISTS idx_admin_bugs_status ON admin_bugs(status);
CREATE INDEX IF NOT EXISTS idx_admin_bugs_priority ON admin_bugs(priority);

-- Comments
COMMENT ON TABLE admin_notes IS 'Project notes and documentation for admins';
COMMENT ON TABLE admin_bugs IS 'Bug tracking for the application';
