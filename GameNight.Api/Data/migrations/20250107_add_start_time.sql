ALTER TABLE board_games
    ADD COLUMN start_time TIME NOT NULL DEFAULT '09:00:00' AFTER max_seats;

-- Ensure existing rows have a value (should be covered by the default).
UPDATE board_games
SET start_time = '09:00:00'
WHERE start_time IS NULL;
