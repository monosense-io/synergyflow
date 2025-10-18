-- Audit Log Schema
-- Story 00.1: Idempotent consumer example

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    user_id VARCHAR(255),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),
    occurred_at TIMESTAMP NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_correlation ON audit_log(correlation_id);
CREATE INDEX idx_audit_log_occurred_at ON audit_log(occurred_at DESC);
CREATE INDEX idx_audit_log_user ON audit_log(user_id);

COMMENT ON TABLE audit_log IS 'Audit trail for all domain events';
COMMENT ON COLUMN audit_log.event_id IS 'Unique event identifier for idempotency';
