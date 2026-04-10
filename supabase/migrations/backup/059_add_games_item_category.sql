-- Add 'games' as a valid item_category for both planning session items and event items

-- Update planning_session_items constraint
ALTER TABLE planning_session_items DROP CONSTRAINT IF EXISTS planning_session_items_item_category_check;
ALTER TABLE planning_session_items ADD CONSTRAINT planning_session_items_item_category_check
    CHECK (item_category IN ('food', 'drinks', 'supplies', 'games', 'other'));

-- Update event_items constraint
ALTER TABLE event_items DROP CONSTRAINT IF EXISTS event_items_item_category_check;
ALTER TABLE event_items ADD CONSTRAINT event_items_item_category_check
    CHECK (item_category IN ('food', 'drinks', 'supplies', 'games', 'other'));
