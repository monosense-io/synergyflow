-- Incident Module Schema
-- Story 00.1: Event System Implementation (Incident example)

CREATE TABLE IF NOT EXISTS incidents (
    id BIGSERIAL PRIMARY KEY,
    incident_id VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('P1_CRITICAL', 'P2_HIGH', 'P3_MEDIUM', 'P4_LOW')),
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED')),
    reported_by VARCHAR(255) NOT NULL,
    assigned_to VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_incidents_incident_id ON incidents(incident_id) WHERE deleted = 0;
CREATE INDEX idx_incidents_status ON incidents(status) WHERE deleted = 0;
CREATE INDEX idx_incidents_priority ON incidents(priority) WHERE deleted = 0;
CREATE INDEX idx_incidents_reported_by ON incidents(reported_by) WHERE deleted = 0;
CREATE INDEX idx_incidents_assigned_to ON incidents(assigned_to) WHERE deleted = 0;
CREATE INDEX idx_incidents_created_at ON incidents(created_at DESC);

COMMENT ON TABLE incidents IS 'Incident tracking (ITSM module)';
COMMENT ON COLUMN incidents.incident_id IS 'Public UUID identifier';
COMMENT ON COLUMN incidents.deleted IS 'Soft delete flag (0=active, 1=deleted)';
