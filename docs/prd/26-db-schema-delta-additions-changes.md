---
id: 26-db-schema-delta-additions-changes
title: 26. DB Schema Delta (Additions & Changes)
version: 8.0
last_updated: 2025-09-03
owner: DBA/Architect
status: Draft
---

## 26. DB Schema Delta (Additions & Changes)

Alter tables (columns)
```sql
-- Problem correlation key
ALTER TABLE problems ADD COLUMN IF NOT EXISTS correlation_key varchar;

-- Change rollback plan & verification
ALTER TABLE changes 
  ADD COLUMN IF NOT EXISTS rollback_plan text,
  ADD COLUMN IF NOT EXISTS rollback_verified boolean DEFAULT false;

-- Service Catalog form schema
ALTER TABLE services ADD COLUMN IF NOT EXISTS form_schema jsonb;

-- Service Request form data & approver
ALTER TABLE service_requests 
  ADD COLUMN IF NOT EXISTS form_data jsonb,
  ADD COLUMN IF NOT EXISTS approved_by varchar;

-- Knowledge expiry and ownership
ALTER TABLE articles 
  ADD COLUMN IF NOT EXISTS owner_id varchar,
  ADD COLUMN IF NOT EXISTS expires_at timestamp;
```

New tables
```sql
-- CMDB core
CREATE TABLE IF NOT EXISTS configuration_items (
  id varchar primary key, ci_type varchar, name varchar, env varchar,
  owner_id varchar, status varchar, created_at timestamp, updated_at timestamp
);
CREATE TABLE IF NOT EXISTS ci_relationships (
  id bigserial primary key, source_ci_id varchar, relationship_type varchar,
  target_ci_id varchar, created_at timestamp
);
CREATE TABLE IF NOT EXISTS ci_baselines (
  id bigserial primary key, ci_id varchar, taken_at timestamp, checksum varchar
);
CREATE TABLE IF NOT EXISTS ci_snapshots (
  id bigserial primary key, ci_id varchar, snapshot_at timestamp, data jsonb
);

-- Routing & Skills
CREATE TABLE IF NOT EXISTS skills (
  id bigserial primary key, name varchar, description text
);
CREATE TABLE IF NOT EXISTS user_skills (
  user_id varchar, skill_id bigint, level int,
  primary key (user_id, skill_id)
);
CREATE TABLE IF NOT EXISTS routing_rules (
  id bigserial primary key, name varchar, conditions jsonb, priority int, active boolean
);
CREATE TABLE IF NOT EXISTS escalation_policies (
  id bigserial primary key, name varchar, levels jsonb, active boolean
);

-- Preferences & Notifications
CREATE TABLE IF NOT EXISTS user_preferences (
  user_id varchar primary key, theme varchar, locale varchar, quiet_hours jsonb
);
CREATE TABLE IF NOT EXISTS notification_preferences (
  principal_type varchar, principal_id varchar, channel varchar, event_type varchar, enabled boolean,
  primary key (principal_type, principal_id, channel, event_type)
);

-- Change Calendar
CREATE TABLE IF NOT EXISTS change_calendar (
  id bigserial primary key, name varchar, timezone varchar
);
CREATE TABLE IF NOT EXISTS blackout_windows (
  id bigserial primary key, calendar_id bigint, starts_at timestamp, ends_at timestamp, scope jsonb
);
```


## Review Checklist
- Content complete and consistent with PRD
- Acceptance criteria traceable to tests (where applicable)
- Data model references validated (where applicable)
- Event interactions documented (where applicable)
- Security/privacy considerations addressed
- Owner reviewed and status updated

## Traceability
- Features → Data Model: see 18-data-model-overview.md
- Features → Events: see 03-system-architecture-monolithic-approach.md
- Features → Security: see 13-security-compliance-framework.md
- Features → Tests: see 11-testing-strategy-spring-modulith.md
