-- Migration V1: Create ITSM OLTP Tables
-- ITSM Module: tickets, incidents, service_requests, ticket_comments, routing_rules
-- Note: Foreign key constraints deferred to V5 (after shared tables created in V3)

-- Table: tickets (parent entity for incidents and service requests)
CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    requester_id UUID NOT NULL, -- FK to users (added in V5)
    assignee_id UUID,            -- FK to users (added in V5)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_tickets_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'PENDING', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT chk_tickets_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'))
);

CREATE INDEX idx_tickets_status ON tickets(status) WHERE status IN ('OPEN', 'IN_PROGRESS', 'PENDING');
CREATE INDEX idx_tickets_requester ON tickets(requester_id);
CREATE INDEX idx_tickets_assignee ON tickets(assignee_id) WHERE assignee_id IS NOT NULL;
CREATE INDEX idx_tickets_created_at ON tickets(created_at DESC);

-- Table: incidents (subtype of ticket)
CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL, -- FK to tickets (added in V5)
    severity VARCHAR(20) NOT NULL,
    impact VARCHAR(100),
    affected_service VARCHAR(200),
    root_cause TEXT,
    resolution TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_incidents_severity CHECK (severity IN ('SEV1', 'SEV2', 'SEV3', 'SEV4'))
);

CREATE INDEX idx_incidents_ticket ON incidents(ticket_id);
CREATE INDEX idx_incidents_severity ON incidents(severity) WHERE severity IN ('SEV1', 'SEV2');

-- Table: service_requests (subtype of ticket)
CREATE TABLE service_requests (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL, -- FK to tickets (added in V5)
    request_type VARCHAR(100) NOT NULL,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approved_by UUID,            -- FK to users (added in V5)
    approved_at TIMESTAMP,
    fulfillment_notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_sr_approval_status CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED', 'FULFILLED'))
);

CREATE INDEX idx_service_requests_ticket ON service_requests(ticket_id);
CREATE INDEX idx_service_requests_approval ON service_requests(approval_status) WHERE approval_status = 'PENDING';

-- Table: ticket_comments
CREATE TABLE ticket_comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL, -- FK to tickets (added in V5)
    author_id UUID NOT NULL, -- FK to users (added in V5)
    comment_text TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id, created_at DESC);
CREATE INDEX idx_ticket_comments_author ON ticket_comments(author_id);

-- Table: routing_rules (ticket assignment automation)
CREATE TABLE routing_rules (
    id UUID PRIMARY KEY,
    rule_name VARCHAR(200) NOT NULL,
    priority_filter VARCHAR(20),
    category_filter VARCHAR(100),
    status_filter VARCHAR(50),
    target_team_id UUID,        -- FK to teams (added in V5)
    target_user_id UUID,         -- FK to users (added in V5)
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_routing_target CHECK (
        (target_team_id IS NOT NULL AND target_user_id IS NULL) OR
        (target_team_id IS NULL AND target_user_id IS NOT NULL)
    )
);

CREATE INDEX idx_routing_rules_active ON routing_rules(is_active) WHERE is_active = true;
CREATE INDEX idx_routing_rules_filters ON routing_rules(priority_filter, category_filter) WHERE is_active = true;
