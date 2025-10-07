-- Migration V4: Create CQRS-Lite Read Models
-- Denormalized views for high-performance queries (<200ms p95)
-- Updated by Event Worker consuming from outbox table

-- Read Model: ticket_card (ITSM queue view with SLA indicator)
CREATE TABLE ticket_card (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE,
    ticket_number VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    category VARCHAR(100),
    requester_name VARCHAR(200) NOT NULL,
    requester_email VARCHAR(255) NOT NULL,
    assignee_name VARCHAR(200),
    assignee_email VARCHAR(255),
    sla_due_at TIMESTAMP,
    sla_status VARCHAR(20),
    is_overdue BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_ticket_card_sla_status CHECK (sla_status IN ('GREEN', 'AMBER', 'RED', 'BREACHED'))
);

CREATE INDEX idx_ticket_card_status ON ticket_card(status) WHERE status IN ('OPEN', 'IN_PROGRESS', 'PENDING');
CREATE INDEX idx_ticket_card_assignee ON ticket_card(assignee_email) WHERE assignee_email IS NOT NULL;
CREATE INDEX idx_ticket_card_sla_due ON ticket_card(sla_due_at NULLS LAST) WHERE sla_due_at IS NOT NULL;
CREATE INDEX idx_ticket_card_overdue ON ticket_card(is_overdue) WHERE is_overdue = true;

-- Read Model: sla_tracking (SLA breach prediction and monitoring)
CREATE TABLE sla_tracking (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE,
    ticket_number VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    sla_target_minutes INTEGER NOT NULL,
    response_due_at TIMESTAMP NOT NULL,
    resolution_due_at TIMESTAMP NOT NULL,
    first_response_at TIMESTAMP,
    resolved_at TIMESTAMP,
    response_breached BOOLEAN NOT NULL DEFAULT false,
    resolution_breached BOOLEAN NOT NULL DEFAULT false,
    time_to_response_minutes INTEGER,
    time_to_resolution_minutes INTEGER,
    paused BOOLEAN NOT NULL DEFAULT false,
    pause_reason VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_sla_tracking_response_due ON sla_tracking(response_due_at) WHERE first_response_at IS NULL AND NOT paused;
CREATE INDEX idx_sla_tracking_resolution_due ON sla_tracking(resolution_due_at) WHERE resolved_at IS NULL AND NOT paused;
CREATE INDEX idx_sla_tracking_breached ON sla_tracking(response_breached, resolution_breached) WHERE response_breached = true OR resolution_breached = true;

-- Read Model: related_incidents (incident relationship graph for major incident management)
CREATE TABLE related_incidents (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL,
    incident_number VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    parent_incident_id UUID,
    parent_incident_number VARCHAR(50),
    child_incident_ids UUID[],
    child_incident_numbers VARCHAR(50)[],
    related_count INTEGER NOT NULL DEFAULT 0,
    affected_service VARCHAR(200),
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_related_incidents_incident ON related_incidents(incident_id);
CREATE INDEX idx_related_incidents_parent ON related_incidents(parent_incident_id) WHERE parent_incident_id IS NOT NULL;
CREATE INDEX idx_related_incidents_severity ON related_incidents(severity) WHERE severity IN ('SEV1', 'SEV2');

-- Read Model: queue_row (agent work queue with smart routing)
CREATE TABLE queue_row (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL UNIQUE,
    ticket_number VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    queue_name VARCHAR(100) NOT NULL,
    queue_position INTEGER NOT NULL,
    assigned_team_name VARCHAR(200),
    wait_time_minutes INTEGER NOT NULL,
    sla_breached BOOLEAN NOT NULL DEFAULT false,
    auto_route_score INTEGER,
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_queue_row_position CHECK (queue_position >= 0)
);

CREATE INDEX idx_queue_row_queue ON queue_row(queue_name, queue_position);
CREATE INDEX idx_queue_row_team ON queue_row(assigned_team_name) WHERE assigned_team_name IS NOT NULL;
CREATE INDEX idx_queue_row_wait_time ON queue_row(wait_time_minutes DESC);

-- Read Model: issue_card (PM board card with sprint and blockers)
CREATE TABLE issue_card (
    id UUID PRIMARY KEY,
    issue_id UUID NOT NULL UNIQUE,
    issue_key VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    issue_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    story_points INTEGER,
    assignee_name VARCHAR(200),
    assignee_email VARCHAR(255),
    sprint_name VARCHAR(200),
    board_column VARCHAR(100),
    is_blocked BOOLEAN NOT NULL DEFAULT false,
    blocker_count INTEGER NOT NULL DEFAULT 0,
    parent_issue_key VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_issue_card_status ON issue_card(status) WHERE status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW');
CREATE INDEX idx_issue_card_assignee ON issue_card(assignee_email) WHERE assignee_email IS NOT NULL;
CREATE INDEX idx_issue_card_sprint ON issue_card(sprint_name) WHERE sprint_name IS NOT NULL;
CREATE INDEX idx_issue_card_blocked ON issue_card(is_blocked) WHERE is_blocked = true;

-- Read Model: sprint_summary (sprint metrics and velocity tracking)
CREATE TABLE sprint_summary (
    id UUID PRIMARY KEY,
    sprint_id UUID NOT NULL UNIQUE,
    sprint_name VARCHAR(200) NOT NULL,
    board_name VARCHAR(200) NOT NULL,
    status VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_story_points INTEGER NOT NULL DEFAULT 0,
    completed_story_points INTEGER NOT NULL DEFAULT 0,
    total_issues INTEGER NOT NULL DEFAULT 0,
    completed_issues INTEGER NOT NULL DEFAULT 0,
    in_progress_issues INTEGER NOT NULL DEFAULT 0,
    blocked_issues INTEGER NOT NULL DEFAULT 0,
    completion_percentage NUMERIC(5,2) NOT NULL DEFAULT 0.0,
    velocity NUMERIC(5,2),
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_sprint_summary_percentage CHECK (completion_percentage >= 0.0 AND completion_percentage <= 100.0)
);

CREATE INDEX idx_sprint_summary_sprint ON sprint_summary(sprint_id);
CREATE INDEX idx_sprint_summary_board ON sprint_summary(board_name);
CREATE INDEX idx_sprint_summary_status ON sprint_summary(status) WHERE status = 'ACTIVE';
CREATE INDEX idx_sprint_summary_dates ON sprint_summary(start_date, end_date);
