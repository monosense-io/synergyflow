-- SynergyFlow Core Schema (V1)
-- Date: 2025-10-06

-- Users (minimal stub for FK integrity)
CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY,
  username TEXT NOT NULL
);

-- ITSM
CREATE TABLE IF NOT EXISTS tickets (
  id UUID PRIMARY KEY,
  ticket_type VARCHAR(20) NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  status VARCHAR(20) NOT NULL,
  priority VARCHAR(20),
  category TEXT,
  requester_id UUID NOT NULL REFERENCES users(id),
  assignee_id UUID,
  version BIGINT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS ticket_comments (
  id UUID PRIMARY KEY,
  ticket_id UUID NOT NULL REFERENCES tickets(id),
  author_id UUID NOT NULL REFERENCES users(id),
  comment_text TEXT NOT NULL,
  is_internal BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS routing_rules (
  id UUID PRIMARY KEY,
  rule_name TEXT NOT NULL,
  condition_type TEXT NOT NULL,
  condition_value TEXT NOT NULL,
  target_team TEXT NOT NULL,
  priority INTEGER,
  enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- PM
CREATE TABLE IF NOT EXISTS sprints (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  goal TEXT,
  capacity_points INTEGER
);

CREATE TABLE IF NOT EXISTS boards (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  project_id UUID NOT NULL
);

CREATE TABLE IF NOT EXISTS board_columns (
  id UUID PRIMARY KEY,
  board_id UUID NOT NULL REFERENCES boards(id),
  name TEXT NOT NULL,
  position INTEGER NOT NULL,
  wip_limit INTEGER
);

CREATE TABLE IF NOT EXISTS issues (
  id UUID PRIMARY KEY,
  type VARCHAR(16) NOT NULL,
  title TEXT NOT NULL,
  description TEXT,
  status VARCHAR(16) NOT NULL,
  priority VARCHAR(16),
  assignee_id UUID,
  reporter_id UUID NOT NULL REFERENCES users(id),
  story_points INTEGER,
  sprint_id UUID REFERENCES sprints(id),
  epic_id UUID,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS issue_links (
  id UUID PRIMARY KEY,
  src_issue_id UUID NOT NULL REFERENCES issues(id),
  dst_issue_id UUID NOT NULL REFERENCES issues(id),
  relation VARCHAR(16) NOT NULL
);

CREATE TABLE IF NOT EXISTS issue_comments (
  id UUID PRIMARY KEY,
  issue_id UUID NOT NULL REFERENCES issues(id),
  author_id UUID NOT NULL REFERENCES users(id),
  text TEXT NOT NULL,
  is_internal BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL
);

-- Read models (denormalized)
CREATE TABLE IF NOT EXISTS ticket_card (
  ticket_id UUID PRIMARY KEY,
  status VARCHAR(20) NOT NULL,
  priority VARCHAR(20),
  assignee TEXT,
  due_in_seconds INTEGER,
  summary TEXT,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS issue_card (
  issue_id UUID PRIMARY KEY,
  status VARCHAR(16) NOT NULL,
  type VARCHAR(16) NOT NULL,
  priority VARCHAR(16),
  assignee TEXT,
  sprint TEXT,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL
);

-- Workflow/Audit
CREATE TABLE IF NOT EXISTS approvals (
  id UUID PRIMARY KEY,
  request_type TEXT NOT NULL,
  target_id UUID NOT NULL,
  status VARCHAR(16) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  decided_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS audit_log (
  id UUID PRIMARY KEY,
  entity_type TEXT NOT NULL,
  entity_id UUID NOT NULL,
  action TEXT NOT NULL,
  actor_id UUID,
  occurred_at TIMESTAMPTZ NOT NULL,
  payload JSONB
);

-- Outbox (eventing)
CREATE TABLE IF NOT EXISTS outbox (
  id BIGSERIAL PRIMARY KEY,
  aggregate_id UUID NOT NULL,
  aggregate_type TEXT NOT NULL,
  event_type TEXT NOT NULL,
  version BIGINT NOT NULL,
  occurred_at TIMESTAMPTZ NOT NULL,
  payload JSONB NOT NULL,
  processed_at TIMESTAMPTZ
);

