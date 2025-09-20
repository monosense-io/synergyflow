---
id: epic-09-dashboards-reports
title: Epic 09 — Dashboards & Reports
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 09 — Dashboards & Reports

## Epic Goal
Enable customizable dashboards and a robust reporting builder with scheduling and distribution (PRD 2.10–2.11).

## Scope
- In scope: Drag-and-drop dashboard widgets, role-based visibility, live tiles, report builder (filters, grouping, charts), scheduling, exports.
- Out of scope: External analytics platforms (Epic 10).

## Key Requirements
- PRD 2.10/2.11: User/admin dashboards; widget auto-refresh; permissions; sharing; report builder; scheduled delivery; export formats; run history.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Data architecture & caching: docs/architecture/data-architecture.md, docs/adrs/0011-cache-store-dragonflydb-over-redis.md
- Event streams for live tiles: docs/adrs/0006-event-publication-partitioning-retention.md

## API & Events Impact
- APIs: docs/api/modules/search.yaml, docs/api/modules/system.yaml
- Events: metrics.updated, report.scheduled.run

## Data Model Impact
- Entities: Dashboard, Widget, ReportDefinition, Schedule, RunHistory.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 01/02/04 for domain data sources; Epic 12 (Security) for row/field level access; Epic 11 (Notifications) for schedule delivery.

## Risks & Mitigation
- Expensive queries → caching and pre-aggregation; guardrails in builder.
- Permission leaks → consistent policy evaluation in data layer.

## Acceptance Criteria (Epic)
- Users build/share dashboards; reports scheduled and delivered; exports available; run history retained.

## Story Seeds
1) Dashboard model and widget framework.
2) Live tile updates via events.
3) Report builder with query guardrails.
4) Scheduler and email delivery.

## Verification & Done
- Performance tests; permission tests; export correctness.

