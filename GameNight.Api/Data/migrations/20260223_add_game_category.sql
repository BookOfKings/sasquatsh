-- Add game category to events
ALTER TABLE events
ADD COLUMN game_category ENUM(
    'strategy',
    'party',
    'cooperative',
    'deck_building',
    'worker_placement',
    'area_control',
    'dice',
    'trivia',
    'role_playing',
    'miniatures',
    'card',
    'family',
    'abstract',
    'other'
) NULL AFTER game_title;

-- Add index for searching by location
CREATE INDEX ix_events_city ON events(city);
CREATE INDEX ix_events_state ON events(state);
CREATE INDEX ix_events_game_category ON events(game_category);
