-- V007: Routing and skills
CREATE TABLE IF NOT EXISTS skills (
  id bigserial primary key,
  name varchar,
  description text
);

CREATE TABLE IF NOT EXISTS user_skills (
  user_id varchar,
  skill_id bigint,
  level int,
  primary key (user_id, skill_id)
);

CREATE TABLE IF NOT EXISTS routing_rules (
  id bigserial primary key,
  name varchar,
  conditions jsonb,
  priority int,
  active boolean
);

CREATE TABLE IF NOT EXISTS escalation_policies (
  id bigserial primary key,
  name varchar,
  levels jsonb,
  active boolean
);

-- Rollback:
-- DROP TABLE IF EXISTS escalation_policies;
-- DROP TABLE IF EXISTS routing_rules;
-- DROP TABLE IF EXISTS user_skills;
-- DROP TABLE IF EXISTS skills;

