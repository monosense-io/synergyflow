---
id: 18-data-model-overview
title: 18. Data Model Overview
version: 8.0
last_updated: 2025-09-03
owner: Architect (Data)
status: Draft
---

## 18. Data Model Overview

### 18.1 Core Entities

**User Management**:
```sql
users: id, username, email, full_name, active, created_at
teams: id, name, description, parent_team_id, created_at
user_teams: user_id, team_id, role, joined_at
roles: id, name, permissions, created_at
```

**ITIL Process Entities (Separate Bounded Contexts)**:
```sql
-- Incident Management Module
incidents: id, title, description, severity, status, affected_service_id,
           created_by, assigned_to, team_id, created_at, updated_at, resolved_at
incident_comments: id, incident_id, user_id, content, created_at
incident_attachments: id, incident_id, filename, file_path, size, created_at

-- Problem Management Module  
problems: id, title, description, root_cause, status, priority, correlation_key,
          created_by, assigned_to, team_id, created_at, updated_at, closed_at
problem_incidents: problem_id, incident_id, relationship_type
known_errors: id, problem_id, title, description, workaround, status

-- Change Management Module
changes: id, title, description, type, status, risk_level, priority, rollback_plan,
         rollback_verified, created_by, assigned_to, team_id, scheduled_date, created_at, updated_at
change_approvals: id, change_id, approver_id, decision, comments, approved_at
change_implementations: id, change_id, implementer_id, status, completed_at

-- Service Request Module
service_requests: id, service_id, title, description, status, priority,
                  requested_by, approved_by, assigned_to, team_id, form_data(jsonb),
                  created_at, updated_at, fulfilled_at
```

**Service Catalog**:
```sql
services: id, name, description, category, status, sla_id, form_schema(jsonb), created_at
slas: id, name, response_time, resolution_time, availability, created_at
```

**Knowledge Base**:
```sql
articles: id, title, content, category, status, owner_id, expires_at, created_by, created_at
article_tags: article_id, tag_id
tags: id, name, color, created_at
```

**CMDB**:
```sql
configuration_items: id, ci_type, name, env, owner_id, status, created_at, updated_at
ci_relationships: id, source_ci_id, relationship_type, target_ci_id, created_at
ci_baselines: id, ci_id, taken_at, checksum
ci_snapshots: id, ci_id, snapshot_at, data(jsonb)
```

**Routing & Skills**:
```sql
skills: id, name, description
user_skills: user_id, skill_id, level
routing_rules: id, name, conditions(jsonb), priority, active
escalation_policies: id, name, levels(jsonb), active
```

**Preferences & Notifications**:
```sql
user_preferences: user_id, theme, locale, quiet_hours(jsonb)
notification_preferences: principal_type, principal_id, channel, event_type, enabled
```

**Change Calendar**:
```sql
change_calendar: id, name, timezone
blackout_windows: id, calendar_id, starts_at, ends_at, scope(jsonb)
```

**Asset Management**:
```sql
assets: id, asset_tag, name, type, status, location, cost, acquired_date
suppliers: id, name, contact_info, contract_id, performance_rating
contracts: id, supplier_id, start_date, end_date, value, terms
```

**Event Management**:
```sql
events: id, source, type, severity, status, correlation_id, created_at
monitors: id, name, target, thresholds, enabled, created_at
alerts: id, monitor_id, event_id, notification_sent, acknowledged_at
```

**Release Management**:
```sql
releases: id, name, version, status, planned_date, actual_date, created_by
builds: id, release_id, version, artifacts, build_status, created_at
environments: id, name, type, status, configuration, last_deployed
```

**Workflow Orchestration**:
```sql
workflows: id, name, definition, version, active, created_at
workflow_instances: id, workflow_id, status, current_step, created_at
business_rules: id, name, conditions, actions, priority, active
```

**Event Publication Registry (Spring Modulith)**:
```sql
-- Spring Modulith JDBC Event Publication Registry with Production Optimizations
CREATE TABLE event_publication (
    id BIGSERIAL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL, 
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP NOT NULL,
    completion_date TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (publication_date);

-- Create monthly partitions for performance
CREATE TABLE event_publication_y2025m09 PARTITION OF event_publication
    FOR VALUES FROM ('2025-09-01') TO ('2025-10-01');
CREATE TABLE event_publication_y2025m10 PARTITION OF event_publication
    FOR VALUES FROM ('2025-10-01') TO ('2025-11-01');
-- Additional monthly partitions created automatically

-- Performance indexes optimized for 2000+ TPS
CREATE INDEX idx_event_publication_incomplete 
    ON event_publication(listener_id, event_type, publication_date) 
    WHERE completion_date IS NULL;
    
CREATE INDEX idx_event_publication_cleanup 
    ON event_publication(completion_date, publication_date) 
    WHERE completion_date IS NOT NULL;
    
-- Index for event replay and debugging
CREATE INDEX idx_event_publication_type_date 
    ON event_publication(event_type, publication_date);
    
-- Event archive table for completed events (when using archive completion mode)
CREATE TABLE event_publication_archive (
    LIKE event_publication INCLUDING ALL
) PARTITION BY RANGE (completion_date);
```

**Performance Indexes**:
```sql
-- Critical performance indexes for ITIL processes
CREATE INDEX idx_incidents_status_team ON incidents(status, team_id, created_at);
CREATE INDEX idx_incidents_assigned ON incidents(assigned_to, status);
CREATE INDEX idx_incidents_sla ON incidents(created_at, severity) WHERE status IN ('OPEN', 'IN_PROGRESS');
CREATE INDEX idx_problems_status ON problems(status, priority, created_at);
CREATE INDEX idx_problems_correlation ON problems(correlation_key, status);
CREATE INDEX idx_changes_scheduled ON changes(scheduled_date, status);
CREATE INDEX idx_changes_approval ON changes(status, risk_level) WHERE status = 'PENDING_APPROVAL';
CREATE INDEX idx_service_requests_team ON service_requests(team_id, status);
CREATE INDEX idx_service_requests_fulfillment ON service_requests(service_id, status, created_at);

-- Spring Modulith event publication performance indexes
CREATE INDEX idx_event_publications_active ON event_publication(listener_id, publication_date) 
    WHERE completion_date IS NULL;
CREATE INDEX idx_event_publications_retry ON event_publication(event_type, publication_date) 
    WHERE completion_date IS NULL AND publication_date < (NOW() - INTERVAL '30 minutes');
CREATE INDEX idx_event_publications_cleanup ON event_publication(completion_date) 
    WHERE completion_date IS NOT NULL AND completion_date < (NOW() - INTERVAL '7 days');
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
