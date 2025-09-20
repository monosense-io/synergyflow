---
id: epic-01-incident-problem
title: Epic 01 — Incident & Problem Management
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 01 — Incident & Problem Management

## Epic Goal
Deliver robust, SLA-driven Incident and Problem Management aligned to PRD 2.1 and 2.2, enabling rapid triage, automated routing, RCA, and prevention of recurrence.

## Scope
- In scope: Incident lifecycle, SLA timers and alerts, auto-routing hooks, audit trail, Problem records, Known Errors, trend analysis, RCA workflow.
- Out of scope: Change/Release workflows (Epic 02), service request catalog (Epic 04).

## Key Requirements
- From PRD 2.1 Incident Management: multi-channel creation (email/portal/mobile), auto-routing by skill/availability, SLA pre-breach alerts, templates, full audit logging.
- From PRD 2.2 Problem Management: link incidents↔problems, Known Error records, RCA at closure, trend reports, governed lifecycle (Draft→Analysis→Resolved→Closed).

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Event-driven comms: docs/adrs/0001-event-only-inter-module-communication.md
- Event contract versioning: docs/adrs/0004-event-contract-versioning.md
- CMDB impact handshake: docs/adrs/0005-cmdb-impact-assessment-handshake.md
- Modulith boundaries/testing: docs/prd/24-spring-modulith-architecture-compliance-summary.md
- Eventing architecture: docs/architecture/eventing-architecture.md

## API & Events Impact
- APIs: docs/api/modules/incidents.yaml, docs/api/modules/problems.yaml, docs/api/modules/search.yaml
- Events: docs/api/modules/events.yaml, event-catalog.md (ticket.created, ticket.routed, problem.created, known-error.published, sla.warning)

## Data Model Impact
- Core entities: Incident, Problem, KnownError, SLA timers, AuditLog.
- Refs: docs/prd/18-data-model-overview.md, docs/prd/26-db-schema-delta-additions-changes.md

## Dependencies
- Epic 06 (Routing) for assignment signals; Epic 11 (Notifications) for alerts; Epic 08 (CMDB) for impact context; Epic 12 (Security) for roles.

## Risks & Mitigation
- Misrouting increases MTTR → A/B test routing strategies and add manual override flows.
- SLA timer drift → use monotonic clocks and persistence-backed timers.
- Incomplete RCA data → enforce closure fields and validation hooks.

## Acceptance Criteria (Epic)
- SLA pre-breach alerts delivered with configurable thresholds; audit trail present for all status transitions.
- Incidents can be created via email, portal, and mobile with attachments; auto-routing assigns within 5 minutes where capacity exists.
- Problems aggregate related incidents with bidirectional links; Known Errors attach workarounds; RCA mandatory at closure.

## Story Seeds
1) Implement incident lifecycle with audit logging.
2) SLA timers and pre-breach notifications.
3) Auto-routing integration hooks and manual override.
4) Problem record creation from incidents; linking rules.
5) Known Error repository with article linkage.
6) Trend reporting for recurring categories/services.

## Verification & Done
- Tests mapped to PRD acceptance criteria (see docs/prd/11-testing-strategy-spring-modulith.md).
- Data model deltas applied and reversible migrations in place.
- Event emissions/consumption validated against event-catalog.

