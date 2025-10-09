-- Migration V10: Modify outbox idempotency index to include event_type
-- This allows multiple event types to be published for the same aggregate and version
-- Required for audit events that are published alongside business events

-- Drop the old unique index
DROP INDEX IF EXISTS idx_outbox_idempotency;

-- Create new unique index including event_type
CREATE UNIQUE INDEX idx_outbox_idempotency ON outbox(aggregate_id, event_type, version);

-- This change ensures that:
-- 1. Idempotency is still maintained per event type
-- 2. Multiple event types (e.g., TicketAssigned + TicketAssignmentAudit) can be published
--    for the same aggregate and version
-- 3. Prevents true duplicates (same aggregate_id, event_type, AND version)
