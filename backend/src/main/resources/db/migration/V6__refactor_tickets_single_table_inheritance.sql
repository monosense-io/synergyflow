-- Migration V6: Refactor tickets to SINGLE_TABLE inheritance (Story 2.1)
-- Changes:
--   1. Add ticket_type discriminator column for SINGLE_TABLE JPA inheritance
--   2. Update status CHECK constraint to match new TicketStatus enum values
--   3. Remove ticket_number column (replaced by ID-based numbering)
--   4. Add subclass-specific columns to tickets table (nullable for SINGLE_TABLE strategy)
--   5. Migrate data from separate incidents/service_requests tables into tickets table
--   6. Drop separate incidents/service_requests tables

-- Step 1: Add ticket_type discriminator column (required for JPA SINGLE_TABLE inheritance)
ALTER TABLE tickets ADD COLUMN ticket_type VARCHAR(50);

-- Step 2: Set default ticket_type for existing rows (assume INCIDENT as default)
UPDATE tickets SET ticket_type = 'INCIDENT' WHERE ticket_type IS NULL;

-- Step 3: Make ticket_type NOT NULL now that all rows have a value
ALTER TABLE tickets ALTER COLUMN ticket_type SET NOT NULL;

-- Step 4: Add subclass-specific columns to tickets table (nullable for SINGLE_TABLE strategy)
-- Incident-specific columns
ALTER TABLE tickets ADD COLUMN severity VARCHAR(20); -- For Incident subclass
ALTER TABLE tickets ADD COLUMN resolution_notes TEXT; -- For Incident subclass
-- Note: resolved_at already exists in tickets table

-- ServiceRequest-specific columns
ALTER TABLE tickets ADD COLUMN request_type VARCHAR(100); -- For ServiceRequest subclass
ALTER TABLE tickets ADD COLUMN approval_status VARCHAR(50); -- For ServiceRequest subclass
ALTER TABLE tickets ADD COLUMN fulfiller_id UUID; -- For ServiceRequest subclass (assignee for fulfillment)
ALTER TABLE tickets ADD COLUMN fulfilled_at TIMESTAMP; -- For ServiceRequest subclass

-- Step 5: Update status CHECK constraint to match new TicketStatus enum
-- Drop old constraint
ALTER TABLE tickets DROP CONSTRAINT IF EXISTS chk_tickets_status;

-- Add new constraint with Story 2.1 TicketStatus enum values
ALTER TABLE tickets ADD CONSTRAINT chk_tickets_status
    CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'));

-- Step 6: Migrate data from incidents table to tickets table (if any data exists)
-- Update tickets with incident-specific data
UPDATE tickets t
SET
    severity = (
        SELECT i.severity
        FROM incidents i
        WHERE i.ticket_id = t.id
        LIMIT 1
    ),
    resolution_notes = (
        SELECT i.resolution
        FROM incidents i
        WHERE i.ticket_id = t.id
        LIMIT 1
    )
WHERE EXISTS (
    SELECT 1 FROM incidents i WHERE i.ticket_id = t.id
);

-- Step 7: Migrate data from service_requests table to tickets table (if any data exists)
-- Update tickets with service_request-specific data
UPDATE tickets t
SET
    ticket_type = 'SERVICE_REQUEST', -- Override default INCIDENT type
    request_type = (
        SELECT sr.request_type
        FROM service_requests sr
        WHERE sr.ticket_id = t.id
        LIMIT 1
    ),
    approval_status = (
        SELECT sr.approval_status
        FROM service_requests sr
        WHERE sr.ticket_id = t.id
        LIMIT 1
    ),
    fulfilled_at = (
        SELECT sr.approved_at
        FROM service_requests sr
        WHERE sr.ticket_id = t.id
        LIMIT 1
    )
WHERE EXISTS (
    SELECT 1 FROM service_requests sr WHERE sr.ticket_id = t.id
);

-- Step 8: Drop foreign key constraints from incidents and service_requests tables (if any)
-- Note: Foreign keys are added in V5, so this might not exist yet depending on migration order
ALTER TABLE incidents DROP CONSTRAINT IF EXISTS fk_incidents_ticket;
ALTER TABLE service_requests DROP CONSTRAINT IF EXISTS fk_service_requests_ticket;

-- Step 9: Drop incidents and service_requests tables (data already migrated to tickets)
DROP TABLE IF EXISTS incidents CASCADE;
DROP TABLE IF EXISTS service_requests CASCADE;

-- Step 10: Drop ticket_number column (Story 2.1 removes this field)
ALTER TABLE tickets DROP COLUMN IF EXISTS ticket_number;

-- Note: Keeping version column default as 1 (project convention, not JPA standard 0)

-- Step 12: Add indexes for new columns
CREATE INDEX idx_tickets_type ON tickets(ticket_type);
CREATE INDEX idx_tickets_severity ON tickets(severity) WHERE severity IS NOT NULL;
CREATE INDEX idx_tickets_approval_status ON tickets(approval_status) WHERE approval_status = 'PENDING';

-- Step 13: Add CHECK constraints for subclass-specific enums
ALTER TABLE tickets ADD CONSTRAINT chk_tickets_severity
    CHECK (severity IS NULL OR severity IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));

ALTER TABLE tickets ADD CONSTRAINT chk_tickets_approval_status
    CHECK (approval_status IS NULL OR approval_status IN ('PENDING', 'APPROVED', 'REJECTED'));

-- Migration complete: tickets table now uses SINGLE_TABLE inheritance with discriminator column
