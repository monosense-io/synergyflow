-- Migration V12: Make ticket priority nullable (Story 2.3)
-- Purpose: Allow incidents to be created without initial priority, with SLA tracking added when priority is set
-- Context: Story 2.3 AC-3 and AC-6 require nullable priority for incidents

-- Remove NOT NULL constraint from tickets.priority
ALTER TABLE tickets ALTER COLUMN priority DROP NOT NULL;

-- Comment explaining the change
COMMENT ON COLUMN tickets.priority IS 'Priority level (CRITICAL, HIGH, MEDIUM, LOW). Nullable to support incidents created without initial priority (Story 2.3). SLA tracking begins when priority is set.';
