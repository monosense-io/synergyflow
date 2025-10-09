-- Migration V11: Create sla_tracking table (Story 2.3)
-- Purpose: Track SLA deadlines for incident tickets based on priority-based durations
-- Context: Only INCIDENT tickets get SLA tracking (service requests use approval workflows)

-- Drop table if exists (for development/test environments with failed migrations)
DROP TABLE IF EXISTS sla_tracking CASCADE;

CREATE TABLE sla_tracking (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE,
    priority VARCHAR(20) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE NOT NULL,
    breached BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT DEFAULT 0 NOT NULL
);

-- Foreign key constraint with cascade delete (SLA record deleted when ticket is deleted)
ALTER TABLE sla_tracking
    ADD CONSTRAINT fk_sla_tracking_ticket
    FOREIGN KEY (ticket_id) REFERENCES tickets(id) ON DELETE CASCADE;

-- Index on ticket_id for O(1) lookup by TicketService.updatePriority()
CREATE INDEX idx_sla_tracking_ticket_id ON sla_tracking(ticket_id);

-- Index on due_at for breach detection queries (future feature)
CREATE INDEX idx_sla_tracking_due_at ON sla_tracking(due_at);

-- CHECK constraint for priority enum values (must match Priority.java enum)
ALTER TABLE sla_tracking
    ADD CONSTRAINT chk_sla_tracking_priority
    CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));

-- Comment on table
COMMENT ON TABLE sla_tracking IS 'SLA tracking records for incident tickets with priority-based deadlines (Story 2.3)';
COMMENT ON COLUMN sla_tracking.ticket_id IS 'Foreign key to tickets.id (INCIDENT tickets only)';
COMMENT ON COLUMN sla_tracking.priority IS 'Priority at time of SLA calculation (CRITICAL=2h, HIGH=4h, MEDIUM=8h, LOW=24h)';
COMMENT ON COLUMN sla_tracking.due_at IS 'SLA deadline calculated from ticket.created_at + priority-based duration';
COMMENT ON COLUMN sla_tracking.breached IS 'TRUE if current time > due_at (updated by future breach detection job)';
COMMENT ON COLUMN sla_tracking.version IS 'Optimistic locking version (incremented on each update)';
