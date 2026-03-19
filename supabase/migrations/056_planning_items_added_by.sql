-- Add added_by_user_id to track who added each item
-- This allows users to delete items they added (not just the session creator)

ALTER TABLE planning_session_items
ADD COLUMN added_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL;

-- Create index for efficient lookups
CREATE INDEX idx_planning_session_items_added_by ON planning_session_items(added_by_user_id) WHERE added_by_user_id IS NOT NULL;

-- Add RLS policy for users to delete items they added
CREATE POLICY "Users can delete items they added"
  ON planning_session_items
  FOR DELETE
  USING (added_by_user_id = get_current_user_id());

COMMENT ON COLUMN planning_session_items.added_by_user_id IS 'User who added this item - can delete their own items';
