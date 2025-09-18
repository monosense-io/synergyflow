-- V006: CMDB core tables
CREATE TABLE IF NOT EXISTS configuration_items (
  id varchar primary key,
  ci_type varchar,
  name varchar,
  env varchar,
  owner_id varchar,
  status varchar,
  created_at timestamp,
  updated_at timestamp
);

CREATE TABLE IF NOT EXISTS ci_relationships (
  id bigserial primary key,
  source_ci_id varchar,
  relationship_type varchar,
  target_ci_id varchar,
  created_at timestamp
);

CREATE TABLE IF NOT EXISTS ci_baselines (
  id bigserial primary key,
  ci_id varchar,
  taken_at timestamp,
  checksum varchar
);

CREATE TABLE IF NOT EXISTS ci_snapshots (
  id bigserial primary key,
  ci_id varchar,
  snapshot_at timestamp,
  data jsonb
);

-- Rollback: drop tables (ensure order respects FKs if added later)
-- DROP TABLE IF EXISTS ci_snapshots;
-- DROP TABLE IF EXISTS ci_baselines;
-- DROP TABLE IF EXISTS ci_relationships;
-- DROP TABLE IF EXISTS configuration_items;

