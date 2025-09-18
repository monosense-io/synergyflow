-- V008: Preferences and notification preferences
CREATE TABLE IF NOT EXISTS user_preferences (
  user_id varchar primary key,
  theme varchar,
  locale varchar,
  quiet_hours jsonb
);

CREATE TABLE IF NOT EXISTS notification_preferences (
  principal_type varchar,
  principal_id varchar,
  channel varchar,
  event_type varchar,
  enabled boolean,
  primary key (principal_type, principal_id, channel, event_type)
);

-- Rollback:
-- DROP TABLE IF EXISTS notification_preferences;
-- DROP TABLE IF EXISTS user_preferences;

