-- V17: Add composite partial index for routing_rules performance
-- Purpose: Optimize queries fetching enabled rules ordered by priority
-- Context: AI-001 Database Index Optimization (Story 2.4)

CREATE INDEX IF NOT EXISTS idx_routing_rules_enabled_priority
    ON routing_rules(enabled, priority)
    WHERE enabled = true;

