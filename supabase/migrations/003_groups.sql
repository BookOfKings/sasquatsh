-- Groups Feature Migration
-- Run this in your Supabase SQL Editor

-- Groups table
CREATE TABLE IF NOT EXISTS groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(2000),
    logo_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    group_type VARCHAR(20) NOT NULL CHECK (group_type IN ('geographic', 'interest', 'both')),
    location_city VARCHAR(100),
    location_state VARCHAR(50),
    location_radius_miles INT,
    is_public BOOLEAN NOT NULL DEFAULT TRUE,
    created_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Group memberships
CREATE TABLE IF NOT EXISTS group_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'admin', 'member')),
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

-- Link games to groups (optional)
ALTER TABLE events ADD COLUMN IF NOT EXISTS group_id UUID REFERENCES groups(id) ON DELETE SET NULL;

-- Recurring game templates (for groups)
CREATE TABLE IF NOT EXISTS recurring_games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time TIME NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 120,
    max_players INT NOT NULL DEFAULT 4,
    location_details VARCHAR(200),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_groups_slug ON groups(slug);
CREATE INDEX IF NOT EXISTS idx_groups_type ON groups(group_type);
CREATE INDEX IF NOT EXISTS idx_groups_public ON groups(is_public);
CREATE INDEX IF NOT EXISTS idx_group_memberships_group ON group_memberships(group_id);
CREATE INDEX IF NOT EXISTS idx_group_memberships_user ON group_memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_events_group ON events(group_id);
CREATE INDEX IF NOT EXISTS idx_recurring_games_group ON recurring_games(group_id);

-- RLS Policies

-- Enable RLS
ALTER TABLE groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE group_memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE recurring_games ENABLE ROW LEVEL SECURITY;

-- Groups policies
CREATE POLICY "Public groups viewable by everyone" ON groups
FOR SELECT USING (is_public = true);

CREATE POLICY "Members can view private groups" ON groups
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM group_memberships
        WHERE group_id = groups.id
        AND user_id = (SELECT id FROM users WHERE firebase_uid = auth.uid()::text)
    )
);

-- Group memberships policies
CREATE POLICY "Memberships viewable for public groups" ON group_memberships
FOR SELECT USING (
    EXISTS (SELECT 1 FROM groups WHERE id = group_id AND is_public = true)
);

CREATE POLICY "Members can view private group memberships" ON group_memberships
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM group_memberships gm
        WHERE gm.group_id = group_memberships.group_id
        AND gm.user_id = (SELECT id FROM users WHERE firebase_uid = auth.uid()::text)
    )
);

-- Recurring games policies
CREATE POLICY "Recurring games viewable for public groups" ON recurring_games
FOR SELECT USING (
    EXISTS (SELECT 1 FROM groups WHERE id = group_id AND is_public = true)
);

CREATE POLICY "Members can view private group recurring games" ON recurring_games
FOR SELECT USING (
    EXISTS (
        SELECT 1 FROM group_memberships
        WHERE group_id = recurring_games.group_id
        AND user_id = (SELECT id FROM users WHERE firebase_uid = auth.uid()::text)
    )
);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_groups_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for updated_at
DROP TRIGGER IF EXISTS groups_updated_at ON groups;
CREATE TRIGGER groups_updated_at
    BEFORE UPDATE ON groups
    FOR EACH ROW
    EXECUTE FUNCTION update_groups_updated_at();

-- Helper function to generate slug from name
CREATE OR REPLACE FUNCTION generate_slug(name TEXT)
RETURNS TEXT AS $$
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
$$ LANGUAGE plpgsql;
