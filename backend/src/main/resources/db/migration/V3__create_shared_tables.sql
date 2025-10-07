-- Migration V3: Create Shared Tables
-- Shared across all modules: users, roles, user_roles, teams, approvals, workflow_states, audit_log, outbox

-- Table: users (application users)
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    keycloak_subject_id VARCHAR(255) UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_keycloak ON users(keycloak_subject_id) WHERE keycloak_subject_id IS NOT NULL;
CREATE INDEX idx_users_active ON users(is_active) WHERE is_active = true;

-- Table: roles (application roles)
CREATE TABLE roles (
    id UUID PRIMARY KEY,
    role_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_roles_name ON roles(role_name);

-- Table: user_roles (many-to-many user-role assignment)
CREATE TABLE user_roles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_user_roles_unique ON user_roles(user_id, role_id);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- Table: teams (organizational teams)
CREATE TABLE teams (
    id UUID PRIMARY KEY,
    team_name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT,
    team_lead_id UUID REFERENCES users(id),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE INDEX idx_teams_name ON teams(team_name);
CREATE INDEX idx_teams_lead ON teams(team_lead_id) WHERE team_lead_id IS NOT NULL;
CREATE INDEX idx_teams_active ON teams(is_active) WHERE is_active = true;

-- Table: approvals (generic approval workflow)
CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    approval_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    requester_id UUID NOT NULL REFERENCES users(id),
    approver_id UUID REFERENCES users(id),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    approval_notes TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 1,
    CONSTRAINT chk_approvals_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_approvals_entity ON approvals(entity_type, entity_id);
CREATE INDEX idx_approvals_status ON approvals(status) WHERE status = 'PENDING';
CREATE INDEX idx_approvals_approver ON approvals(approver_id) WHERE approver_id IS NOT NULL;

-- Table: workflow_states (state machine for domain entities)
CREATE TABLE workflow_states (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    current_state VARCHAR(100) NOT NULL,
    previous_state VARCHAR(100),
    transitioned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    transitioned_by UUID REFERENCES users(id),
    transition_reason TEXT,
    version BIGINT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_workflow_states_entity ON workflow_states(entity_type, entity_id);
CREATE INDEX idx_workflow_states_current ON workflow_states(entity_type, current_state);

-- Table: audit_log (immutable audit trail)
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(50) NOT NULL,
    actor_id UUID REFERENCES users(id),
    actor_ip VARCHAR(45),
    changes JSONB,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'STATE_CHANGE', 'ACCESS'))
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log(actor_id, occurred_at DESC) WHERE actor_id IS NOT NULL;
CREATE INDEX idx_audit_log_occurred ON audit_log(occurred_at DESC);

-- Table: outbox (transactional outbox pattern for event publishing)
CREATE TABLE outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    version BIGINT NOT NULL,
    occurred_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    processing_error TEXT,
    retry_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT chk_outbox_retry CHECK (retry_count >= 0 AND retry_count <= 10)
);

CREATE UNIQUE INDEX idx_outbox_idempotency ON outbox(aggregate_id, version);
CREATE INDEX idx_outbox_unprocessed ON outbox(occurred_at) WHERE processed_at IS NULL;
CREATE INDEX idx_outbox_failed ON outbox(retry_count, occurred_at) WHERE processed_at IS NULL AND retry_count > 0;
