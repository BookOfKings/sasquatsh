-- Planning Session Items (things to bring during planning phase)
-- These items will be copied to the event when the session is finalized

CREATE TABLE IF NOT EXISTS planning_session_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES planning_sessions(id) ON DELETE CASCADE,
    item_name VARCHAR(100) NOT NULL,
    item_category VARCHAR(20) DEFAULT 'other' NOT NULL
        CHECK (item_category IN ('food', 'drinks', 'supplies', 'other')),
    quantity_needed INTEGER DEFAULT 1 NOT NULL,
    claimed_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    claimed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_planning_session_items_session ON planning_session_items(session_id);
CREATE INDEX IF NOT EXISTS idx_planning_session_items_claimed ON planning_session_items(claimed_by_user_id) WHERE claimed_by_user_id IS NOT NULL;

-- Comment
COMMENT ON TABLE planning_session_items IS 'Items to bring for a planning session, copied to event on finalization';
