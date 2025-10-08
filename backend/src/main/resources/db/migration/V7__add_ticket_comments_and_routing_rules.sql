-- Migration V7: Refactor ticket_comments and routing_rules for Story 2.1 Task 3
-- Changes:
--   1. Refactor ticket_comments table to match TicketComment entity (rename author_id to author, drop updated_at/version)
--   2. Refactor routing_rules table to match RoutingRule entity (add condition_type/condition_value/priority, rename columns)
--   3. Update indexes and constraints

-- ============================================================================
-- PART 1: Refactor ticket_comments table
-- ============================================================================

-- Step 1: Drop existing indexes on ticket_comments
DROP INDEX IF EXISTS idx_ticket_comments_ticket;
DROP INDEX IF EXISTS idx_ticket_comments_author;

-- Step 2: Rename author_id to author (UUIDs are interchangeable, just a naming change)
ALTER TABLE ticket_comments RENAME COLUMN author_id TO author;

-- Step 3: Drop updated_at column (comments are immutable in new design)
ALTER TABLE ticket_comments DROP COLUMN IF EXISTS updated_at;

-- Step 4: Drop version column (comments are immutable, no optimistic locking needed)
ALTER TABLE ticket_comments DROP COLUMN IF EXISTS version;

-- Step 5: Ensure created_at is NOT NULL (should already be, but ensure consistency)
ALTER TABLE ticket_comments ALTER COLUMN created_at SET NOT NULL;
ALTER TABLE ticket_comments ALTER COLUMN created_at DROP DEFAULT; -- Remove DEFAULT (set by @PrePersist)

-- Step 6: Add new indexes for ticket_comments
CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);

-- ============================================================================
-- PART 2: Refactor routing_rules table
-- ============================================================================

-- Step 7: Drop existing indexes on routing_rules
DROP INDEX IF EXISTS idx_routing_rules_active;
DROP INDEX IF EXISTS idx_routing_rules_filters;

-- Step 8: Drop old constraint (will be replaced)
ALTER TABLE routing_rules DROP CONSTRAINT IF EXISTS chk_routing_target;

-- Step 9: Add new columns for Story 2.1 routing rule design
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS condition_type VARCHAR(50);
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS condition_value VARCHAR(100);
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS priority INTEGER;
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS target_agent_id UUID;
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT true;
ALTER TABLE routing_rules ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Step 10: Migrate data from old columns to new columns (for any existing data)
-- Set default condition_type = CATEGORY for existing rules with category_filter
UPDATE routing_rules
SET condition_type = 'CATEGORY',
    condition_value = category_filter,
    priority = 100, -- Default priority for migrated rules
    enabled = is_active,
    updated_at = created_at
WHERE condition_type IS NULL AND category_filter IS NOT NULL;

-- Set default condition_type = PRIORITY for existing rules with priority_filter
UPDATE routing_rules
SET condition_type = 'PRIORITY',
    condition_value = priority_filter,
    priority = 100,
    enabled = is_active,
    updated_at = created_at
WHERE condition_type IS NULL AND priority_filter IS NOT NULL AND category_filter IS NULL;

-- Step 11: Rename target_user_id to target_agent_id (already added above, migrate data)
UPDATE routing_rules
SET target_agent_id = target_user_id
WHERE target_user_id IS NOT NULL AND target_agent_id IS NULL;

-- Step 12: Drop old columns from routing_rules
ALTER TABLE routing_rules DROP COLUMN IF EXISTS priority_filter;
ALTER TABLE routing_rules DROP COLUMN IF EXISTS category_filter;
ALTER TABLE routing_rules DROP COLUMN IF EXISTS status_filter;
ALTER TABLE routing_rules DROP COLUMN IF EXISTS target_user_id;
ALTER TABLE routing_rules DROP COLUMN IF EXISTS is_active;

-- Step 13: Make new columns NOT NULL where required
ALTER TABLE routing_rules ALTER COLUMN condition_type SET NOT NULL;
ALTER TABLE routing_rules ALTER COLUMN priority SET NOT NULL;
ALTER TABLE routing_rules ALTER COLUMN enabled SET NOT NULL;
ALTER TABLE routing_rules ALTER COLUMN updated_at SET NOT NULL;

-- Step 14: Remove DEFAULT from timestamps (set by @PrePersist/@PreUpdate)
ALTER TABLE routing_rules ALTER COLUMN created_at DROP DEFAULT;
ALTER TABLE routing_rules ALTER COLUMN updated_at DROP DEFAULT;

-- Step 15: Add new indexes for routing_rules
CREATE INDEX idx_routing_rules_enabled ON routing_rules(enabled) WHERE enabled = true;
CREATE INDEX idx_routing_rules_priority ON routing_rules(priority);
CREATE INDEX idx_routing_rules_condition_type ON routing_rules(condition_type);

-- Step 16: Add CHECK constraints for routing_rules enums
ALTER TABLE routing_rules ADD CONSTRAINT chk_routing_rules_condition_type
    CHECK (condition_type IN ('CATEGORY', 'PRIORITY', 'SUBCATEGORY', 'ROUND_ROBIN'));

-- Step 17: Add CHECK constraint to ensure either targetTeamId or targetAgentId is set
ALTER TABLE routing_rules ADD CONSTRAINT chk_routing_rules_target
    CHECK (target_team_id IS NOT NULL OR target_agent_id IS NOT NULL);

-- Migration complete: ticket_comments and routing_rules tables refactored for Story 2.1
