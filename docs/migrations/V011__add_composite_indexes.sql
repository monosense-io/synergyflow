-- Migration: V011__add_composite_indexes.sql
-- Purpose: Add composite indexes for common dashboard and query patterns
-- Date: 2025-10-17
-- Impact: Improves query performance for SLA monitoring and dashboard queries

-- ============================================================================
-- INCIDENT MODULE COMPOSITE INDEXES
-- ============================================================================

-- Composite index for SLA timer enforcement queries
-- Used by: SLA timer background jobs, dashboard SLA breach warnings
-- Query pattern: SELECT * FROM incidents WHERE status != 'CLOSED' AND sla_deadline < NOW()
CREATE INDEX idx_incidents_status_sla_deadline
    ON synergyflow_incidents.incidents(status, sla_deadline)
    WHERE status != 'CLOSED'
    AND sla_deadline IS NOT NULL;

-- Composite index for dashboard time-series queries
-- Used by: Incident creation trends, incident by status distribution
-- Query pattern: SELECT status, COUNT(*) FROM incidents WHERE created_at > NOW() - INTERVAL '30 days' GROUP BY status
CREATE INDEX idx_incidents_created_at_status
    ON synergyflow_incidents.incidents(created_at DESC, status)
    WHERE status != 'CLOSED';

-- Composite index for assignment filtering
-- Used by: Agent workload dashboard, "my incidents" queries
-- Query pattern: SELECT * FROM incidents WHERE assigned_to = ? AND status NOT IN ('CLOSED', 'RESOLVED')
CREATE INDEX idx_incidents_assigned_to_status
    ON synergyflow_incidents.incidents(assigned_to, status)
    WHERE assigned_to IS NOT NULL;

-- Composite index for priority-based filtering (critical incidents first)
-- Used by: Incident list sorted by priority/SLA
-- Query pattern: SELECT * FROM incidents WHERE status = 'NEW' OR status = 'ASSIGNED' ORDER BY priority DESC, created_at
CREATE INDEX idx_incidents_priority_created_at
    ON synergyflow_incidents.incidents(priority, created_at DESC)
    WHERE status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS');

-- ============================================================================
-- CHANGE MODULE COMPOSITE INDEXES
-- ============================================================================

-- Composite index for change calendar queries
-- Used by: Change calendar view, conflict detection
-- Query pattern: SELECT * FROM changes WHERE status IN ('SCHEDULED', 'IN_PROGRESS') AND scheduled_date BETWEEN ? AND ?
CREATE INDEX idx_changes_status_scheduled_date
    ON synergyflow_changes.changes(status, scheduled_date DESC)
    WHERE status IN ('SCHEDULED', 'IN_PROGRESS');

-- Composite index for change approval routing
-- Used by: Pending approvals list, change approval dashboard
-- Query pattern: SELECT * FROM changes WHERE status = 'PENDING_APPROVAL' ORDER BY created_at
CREATE INDEX idx_changes_status_created_at
    ON synergyflow_changes.changes(status, created_at DESC)
    WHERE status = 'PENDING_APPROVAL';

-- Composite index for risk-based filtering
-- Used by: High-risk changes dashboard, emergency change handling
-- Query pattern: SELECT * FROM changes WHERE risk = 'HIGH' OR risk = 'EMERGENCY' AND status NOT IN ('COMPLETED', 'CANCELLED')
CREATE INDEX idx_changes_risk_status
    ON synergyflow_changes.changes(risk, status)
    WHERE risk IN ('HIGH', 'EMERGENCY');

-- ============================================================================
-- TASK MODULE COMPOSITE INDEXES
-- ============================================================================

-- Composite index for sprint board queries
-- Used by: Sprint board view, task status distribution
-- Query pattern: SELECT * FROM tasks WHERE project_id = ? AND sprint_id = ? ORDER BY status
CREATE INDEX idx_tasks_project_sprint_status
    ON synergyflow_tasks.tasks(project_id, status)
    WHERE status IN ('NOT_STARTED', 'IN_PROGRESS', 'DONE');

-- Composite index for assigned task queries
-- Used by: "My tasks" list, agent workload dashboard
-- Query pattern: SELECT * FROM tasks WHERE assigned_to = ? AND status != 'DONE' ORDER BY created_at
CREATE INDEX idx_tasks_assigned_to_status
    ON synergyflow_tasks.tasks(assigned_to, status)
    WHERE assigned_to IS NOT NULL AND status != 'DONE';

-- ============================================================================
-- AUDIT MODULE COMPOSITE INDEXES
-- ============================================================================

-- Composite index for audit trail queries by entity and time
-- Used by: Audit log viewer, compliance reporting
-- Query pattern: SELECT * FROM audit_events WHERE entity_type = ? AND entity_id = ? ORDER BY occurred_at DESC
CREATE INDEX idx_audit_events_entity_occurred_at
    ON synergyflow_audit.audit_events(entity_type, entity_id, occurred_at DESC);

-- Composite index for user action history
-- Used by: User activity audit, security investigations
-- Query pattern: SELECT * FROM audit_events WHERE user_id = ? AND action = ? ORDER BY occurred_at DESC
CREATE INDEX idx_audit_events_user_action
    ON synergyflow_audit.audit_events(user_id, action, occurred_at DESC);

-- Composite index for policy decision tracking
-- Used by: Policy decision audit, decision receipt history
-- Query pattern: SELECT * FROM policy_decisions WHERE policy_name = ? AND evaluated_at > ? ORDER BY evaluated_at DESC
CREATE INDEX idx_policy_decisions_name_time
    ON synergyflow_audit.policy_decisions(policy_name, evaluated_at DESC)
    WHERE evaluated_at > NOW() - INTERVAL '90 days';

-- ============================================================================
-- KNOWLEDGE MODULE COMPOSITE INDEXES
-- ============================================================================

-- Composite index for article lifecycle queries
-- Used by: Article approval queue, published articles list
-- Query pattern: SELECT * FROM articles WHERE status = 'DRAFT' OR status = 'REVIEW' ORDER BY created_at
CREATE INDEX idx_articles_status_created_at
    ON synergyflow_knowledge.articles(status, created_at DESC)
    WHERE status IN ('DRAFT', 'REVIEW', 'PUBLISHED');

-- Composite index for expiring articles
-- Used by: Article expiry notifications, expiration scheduler
-- Query pattern: SELECT * FROM articles WHERE expires_at < NOW() + INTERVAL '30 days' AND status = 'PUBLISHED'
CREATE INDEX idx_articles_expires_status
    ON synergyflow_knowledge.articles(expires_at ASC, status)
    WHERE status = 'PUBLISHED' AND expires_at IS NOT NULL;

-- ============================================================================
-- PERFORMANCE MONITORING NOTES
-- ============================================================================

-- After creating indexes, monitor the following queries for performance improvements:
-- - SLA timer queries (should improve by 50-70%)
-- - Dashboard status distributions (should improve by 30-50%)
-- - Incident list filtering (should improve by 40-60%)
-- - Audit trail queries (should improve by 60-80%)
--
-- Monitor index usage via:
-- SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
-- FROM pg_stat_user_indexes
-- WHERE idx_scan > 0
-- ORDER BY idx_tup_read DESC;

-- ============================================================================
-- MAINTENANCE
-- ============================================================================

-- These indexes should be analyzed and rebuilt periodically:
-- Weekly: ANALYZE TABLE indices after data loads
-- Monthly: REINDEX on indexes with high bloat
-- Quarterly: Review slow query logs and add new indexes as needed

-- To check index bloat:
-- SELECT schemaname, tablename, indexname, pg_size_pretty(pg_relation_size(idx)) AS idx_size
-- FROM pg_stat_user_indexes
-- ORDER BY pg_relation_size(idx) DESC;

-- To vacuum and analyze:
-- VACUUM ANALYZE synergyflow_incidents.incidents;
-- VACUUM ANALYZE synergyflow_changes.changes;
-- VACUUM ANALYZE synergyflow_tasks.tasks;
-- VACUUM ANALYZE synergyflow_audit.audit_events;
-- VACUUM ANALYZE synergyflow_knowledge.articles;
