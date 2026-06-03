-- Event Game Suggestions + Voting
-- Allows attendees to suggest games on existing events and vote on them
-- Host can approve suggestions into confirmed event_games

CREATE TABLE IF NOT EXISTS event_game_suggestions (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    suggested_by_user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bgg_id integer,
    game_name varchar(200) NOT NULL,
    thumbnail_url text,
    min_players integer,
    max_players integer,
    playing_time integer,
    created_at timestamptz DEFAULT now() NOT NULL
);

CREATE INDEX idx_event_game_suggestions_event ON event_game_suggestions(event_id);
CREATE UNIQUE INDEX idx_event_game_suggestions_bgg_unique ON event_game_suggestions(event_id, bgg_id) WHERE bgg_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS event_game_votes (
    id uuid DEFAULT gen_random_uuid() NOT NULL PRIMARY KEY,
    suggestion_id uuid NOT NULL REFERENCES event_game_suggestions(id) ON DELETE CASCADE,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(suggestion_id, user_id)
);

CREATE INDEX idx_event_game_votes_suggestion ON event_game_votes(suggestion_id);
