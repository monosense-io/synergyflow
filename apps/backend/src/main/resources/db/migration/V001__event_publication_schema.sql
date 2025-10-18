-- Event Publication Schema (Transactional Outbox)
-- Story 00.1: Event System Implementation
--
-- Creates event_publication table for Spring Modulith JDBC event registry
-- and custom tables for idempotency tracking.

-- event_publication table: Spring Modulith transactional outbox
-- Schema based on Spring Modulith JDBC event publication registry
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL DEFAULT NOW(),
    completion_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS event_publication_by_completion_date
    ON event_publication(completion_date, publication_date);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash
    ON event_publication USING HASH (MD5(serialized_event));

-- processed_events table: Idempotency tracking for event consumers
-- Consumers check this table before processing to prevent duplicate side-effects
CREATE TABLE IF NOT EXISTS processed_events (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(512) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    correlation_id VARCHAR(255),
    causation_id VARCHAR(255),

    -- Optional: Store event data for audit/debugging
    event_data JSONB
);

-- Index for correlation ID queries
CREATE INDEX idx_processed_events_correlation
    ON processed_events(correlation_id);

-- Index for time-based queries (cleanup, audit)
CREATE INDEX idx_processed_events_time
    ON processed_events(processed_at DESC);

COMMENT ON TABLE processed_events IS 'Idempotency tracking to prevent duplicate event processing';
COMMENT ON COLUMN processed_events.idempotency_key IS 'Unique key per event+consumer, typically: eventId-listenerClassName';
