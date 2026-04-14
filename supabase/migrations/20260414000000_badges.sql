-- Badges: achievement definitions
CREATE TABLE badges (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug VARCHAR(60) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  description TEXT NOT NULL,
  icon_svg TEXT, -- inline SVG for the badge icon
  category VARCHAR(30) NOT NULL, -- hosting, attendance, planning, social, collection, game_system, items, special
  tier VARCHAR(10) NOT NULL DEFAULT 'bronze', -- bronze, silver, gold, platinum
  requirement_type VARCHAR(40) NOT NULL, -- e.g. games_hosted, games_attended, groups_joined, etc.
  requirement_count INT NOT NULL DEFAULT 1, -- threshold to earn
  is_active BOOLEAN NOT NULL DEFAULT true,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User earned badges
CREATE TABLE user_badges (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  badge_id BIGINT NOT NULL REFERENCES badges(id) ON DELETE CASCADE,
  earned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_pinned BOOLEAN NOT NULL DEFAULT false, -- user can pin up to 3 badges on their avatar
  UNIQUE(user_id, badge_id)
);

CREATE INDEX idx_user_badges_user_id ON user_badges(user_id);
CREATE INDEX idx_user_badges_badge_id ON user_badges(badge_id);

-- RLS
ALTER TABLE badges ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_badges ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read badges" ON badges FOR SELECT USING (true);
CREATE POLICY "Anyone can read user badges" ON user_badges FOR SELECT USING (true);

-- =============================================
-- SEED BADGE DEFINITIONS
-- =============================================

-- HOSTING BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('first_host', 'First Host', 'Hosted your first game night', 'hosting', 'bronze', 'games_hosted', 1, 10),
  ('host_5', 'Regular Host', 'Hosted 5 game nights', 'hosting', 'bronze', 'games_hosted', 5, 11),
  ('host_10', 'Dedicated Host', 'Hosted 10 game nights', 'hosting', 'silver', 'games_hosted', 10, 12),
  ('host_25', 'Veteran Host', 'Hosted 25 game nights', 'hosting', 'silver', 'games_hosted', 25, 13),
  ('host_50', 'Elite Host', 'Hosted 50 game nights', 'hosting', 'gold', 'games_hosted', 50, 14),
  ('host_100', 'Legendary Host', 'Hosted 100 game nights', 'hosting', 'platinum', 'games_hosted', 100, 15),
  ('host_streak_4', 'Consistent Host', 'Hosted games 4 weeks in a row', 'hosting', 'bronze', 'host_streak_weeks', 4, 16),
  ('host_streak_12', 'Iron Host', 'Hosted games 12 weeks in a row', 'hosting', 'silver', 'host_streak_weeks', 12, 17),
  ('multi_table_host', 'Grand Host', 'Hosted a multi-table event', 'hosting', 'silver', 'multi_table_events_hosted', 1, 18);

-- ATTENDANCE BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('first_attend', 'Player One', 'Attended your first game night', 'attendance', 'bronze', 'games_attended', 1, 20),
  ('attend_5', 'Regular Player', 'Attended 5 game nights', 'attendance', 'bronze', 'games_attended', 5, 21),
  ('attend_10', 'Dedicated Player', 'Attended 10 game nights', 'attendance', 'silver', 'games_attended', 10, 22),
  ('attend_25', 'Veteran Player', 'Attended 25 game nights', 'attendance', 'silver', 'games_attended', 25, 23),
  ('attend_50', 'Elite Player', 'Attended 50 game nights', 'attendance', 'gold', 'games_attended', 50, 24),
  ('attend_100', 'Legendary Player', 'Attended 100 game nights', 'attendance', 'platinum', 'games_attended', 100, 25),
  ('attend_streak_4', 'Reliable', 'Attended games 4 weeks in a row', 'attendance', 'bronze', 'attend_streak_weeks', 4, 26),
  ('attend_streak_12', 'Iron Player', 'Attended games 12 weeks in a row', 'attendance', 'silver', 'attend_streak_weeks', 12, 27),
  ('perfect_attendance', 'Perfect Attendance', 'Never cancelled a registration', 'attendance', 'gold', 'zero_cancellations', 1, 28);

-- PLANNING BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('first_plan', 'Planner', 'Created your first planning session', 'planning', 'bronze', 'plans_created', 1, 30),
  ('plan_pro', 'Planning Pro', 'Created 10 planning sessions', 'planning', 'silver', 'plans_created', 10, 31),
  ('vote_champ', 'Vote Champion', 'Voted in 10 planning sessions', 'planning', 'silver', 'plan_votes_cast', 10, 32);

-- SOCIAL BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('group_founder', 'Group Founder', 'Created a group', 'social', 'bronze', 'groups_created', 1, 40),
  ('social_butterfly', 'Social Butterfly', 'Joined 3 groups', 'social', 'bronze', 'groups_joined', 3, 41),
  ('social_butterfly_5', 'Community Pillar', 'Joined 5 groups', 'social', 'silver', 'groups_joined', 5, 42),
  ('inviter_5', 'Inviter', 'Invited 5 people to events', 'social', 'bronze', 'invites_sent', 5, 43),
  ('inviter_25', 'Recruiter', 'Invited 25 people to events', 'social', 'silver', 'invites_sent', 25, 44),
  ('connector', 'Connector', 'Introduced 5 new players via invites', 'social', 'gold', 'new_players_invited', 5, 45);

-- COLLECTION BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('collector_5', 'Collector', 'Added 5 games to your collection', 'collection', 'bronze', 'collection_size', 5, 50),
  ('collector_25', 'Enthusiast', 'Added 25 games to your collection', 'collection', 'silver', 'collection_size', 25, 51),
  ('curator', 'Curator', '50+ games in your collection', 'collection', 'gold', 'collection_size', 50, 52),
  ('hoarder', 'Hoarder', '100+ games in your collection', 'collection', 'platinum', 'collection_size', 100, 53),
  ('public_library', 'Public Library', 'Made your collection public', 'collection', 'bronze', 'collection_public', 1, 54);

-- GAME SYSTEM BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('board_gamer', 'Board Gamer', 'Hosted or attended 5 board game events', 'game_system', 'bronze', 'board_game_events', 5, 60),
  ('planeswalker', 'Planeswalker', 'Hosted or attended 5 MTG events', 'game_system', 'bronze', 'mtg_events', 5, 61),
  ('trainer', 'Trainer', 'Hosted or attended 5 Pokémon events', 'game_system', 'bronze', 'pokemon_events', 5, 62),
  ('duelist', 'Duelist', 'Hosted or attended 5 Yu-Gi-Oh! events', 'game_system', 'bronze', 'yugioh_events', 5, 63),
  ('commander', 'Commander', 'Hosted or attended 5 Warhammer 40k events', 'game_system', 'bronze', 'warhammer_events', 5, 64);

-- ITEMS BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('contributor', 'Contributor', 'Brought items to 3 events', 'items', 'bronze', 'events_with_items', 3, 70),
  ('mvp_contributor', 'MVP Contributor', 'Brought items to 10 events', 'items', 'silver', 'events_with_items', 10, 71),
  ('game_lender', 'Game Lender', 'Brought games to 5 events', 'items', 'bronze', 'events_games_brought', 5, 72),
  ('snack_master', 'Snack Master', 'Brought snacks to 5 events', 'items', 'bronze', 'events_snacks_brought', 5, 73);

-- SPECIAL BADGES
INSERT INTO badges (slug, name, description, category, tier, requirement_type, requirement_count, sort_order) VALUES
  ('founding_member', 'Founding Member', 'One of the first Sasquatsh users', 'special', 'gold', 'is_founding_member', 1, 90),
  ('early_adopter', 'Early Adopter', 'Signed up in the first month', 'special', 'silver', 'early_signup', 1, 91),
  ('bug_reporter', 'Bug Reporter', 'Submitted a bug report', 'special', 'bronze', 'bugs_submitted', 1, 92);
