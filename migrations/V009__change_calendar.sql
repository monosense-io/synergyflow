-- V009: Change calendar and blackout windows
CREATE TABLE IF NOT EXISTS change_calendar (
  id bigserial primary key,
  name varchar,
  timezone varchar
);

CREATE TABLE IF NOT EXISTS blackout_windows (
  id bigserial primary key,
  calendar_id bigint,
  starts_at timestamp,
  ends_at timestamp,
  scope jsonb
);

-- Rollback:
-- DROP TABLE IF EXISTS blackout_windows;
-- DROP TABLE IF EXISTS change_calendar;

