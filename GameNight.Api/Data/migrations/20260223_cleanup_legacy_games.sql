-- Cleanup legacy board game data (no longer needed after event)
-- Run this to clear old game night data

-- Delete reservations first (foreign key constraint)
DELETE FROM reservations;

-- Delete all board games
DELETE FROM board_games;

-- Verify cleanup
SELECT 'board_games' as table_name, COUNT(*) as remaining FROM board_games
UNION ALL
SELECT 'reservations' as table_name, COUNT(*) as remaining FROM reservations;
