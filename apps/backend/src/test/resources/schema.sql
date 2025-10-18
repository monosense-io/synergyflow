CREATE TABLE IF NOT EXISTS processed_events (
  idempotency_key VARCHAR(255) PRIMARY KEY,
  event_type VARCHAR(512),
  correlation_id VARCHAR(64),
  causation_id VARCHAR(64),
  processed_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  entity_type VARCHAR(64) NOT NULL,
  entity_id VARCHAR(128) NOT NULL,
  action VARCHAR(64) NOT NULL,
  details TEXT,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS incidents (
  id BIGSERIAL PRIMARY KEY,
  incident_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  priority VARCHAR(32) NOT NULL,
  status VARCHAR(32) NOT NULL,
  reported_by VARCHAR(64) NOT NULL,
  assigned_to VARCHAR(64),
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP NOT NULL,
  deleted INT NOT NULL DEFAULT 0
);
