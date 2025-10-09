-- Migration V9: Add ticket performance indexes (Story 2.2 Task 13)
-- Purpose:
--   1) Replace legacy status partial index from V1 with an index aligned to the new status set
--      introduced in V6 (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED).
--   2) Ensure commonly filtered columns (assignee_id, requester_id, created_at) are indexed.
--   3) Add a composite index on (status, priority) to speed up dashboard and queue queries
--      that filter by status and priority together.

-- Drop legacy status index from V1 (used outdated status values)
DROP INDEX IF EXISTS idx_tickets_status;

-- Status index aligned with V6 status set
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);

-- Ensure index on assignee (partial for non-null values)
CREATE INDEX IF NOT EXISTS idx_tickets_assignee ON tickets(assignee_id) WHERE assignee_id IS NOT NULL;

-- Ensure index on requester
CREATE INDEX IF NOT EXISTS idx_tickets_requester ON tickets(requester_id);

-- Ensure index on created_at to support time-ordered queries
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at DESC);

-- Composite index for status + priority filters
CREATE INDEX IF NOT EXISTS idx_tickets_status_priority ON tickets(status, priority);

-- Notes:
-- - This migration is safe to run repeatedly due to IF NOT EXISTS.
-- - If this migration is introduced after V10 in some environments, enable Flyway
--   out-of-order for one run or perform a Flyway repair to register it retroactively.

