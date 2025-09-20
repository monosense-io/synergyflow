---
id: epic-02-change-release
title: Epic 02 — Change & Release Management
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 02 — Change & Release Management

## Epic Goal
Establish governed Change and Release Management with visual workflows, gating, calendars, and auditability per PRD 2.3–2.4.

## Scope
- In scope: Flowable-based workflow builder, change lifecycle, approvals/CAB, blackout/calendar conflict checks, release assembly with stage gates, notifications, rollback plans.
- Out of scope: Incident/Problem handling (Epic 01), analytics beyond release KPIs (Epic 09/10).

## Key Requirements
- PRD 2.3: Workflow builder (BPMN 2.0), versioned deployments to Flowable; approvals based on risk; mandatory rollback for med/high risk; audit logging.
- PRD 2.4: Releases link changes/problems/incidents; stage gates enforce readiness; track deployments with status/timestamps; report success rate and MTTR.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Workflow orchestration: docs/architecture/workflow-flowable.md, docs/adrs/0012-workflow-orchestration-flowable-7.md
- Event-driven comms + versioning: docs/adrs/0001-event-only-inter-module-communication.md, docs/adrs/0004-event-contract-versioning.md
- CMDB impact handshake: docs/adrs/0005-cmdb-impact-assessment-handshake.md

## API & Events Impact
- APIs: docs/api/modules/changes.yaml, docs/api/modules/incidents.yaml, docs/api/modules/problems.yaml, docs/api/modules/system.yaml
- Events: change.submitted, change.approved, release.created, release.gate.passed/failed, deployment.started/completed

## Data Model Impact
- Entities: Change, Approval, RiskProfile, Release, ReleaseGate, Deployment, CalendarBlackout, AuditLog.
- Refs: docs/prd/18-data-model-overview.md, docs/prd/26-db-schema-delta-additions-changes.md

## Dependencies
- Epic 08 (CMDB) for impact/risk signals, Epic 11 (Notifications), Epic 14 (Testing & Quality Gates) for readiness signals, Epic 12 (Security) for approvals.

## Risks & Mitigation
- Gate bypass risk → enforce server-side policy with immutable audit and environment checks.
- Calendar false positives → configurable conflict rules and manual overrides with audit.
- Workflow sprawl → template library and version lifecycle governance.

## Acceptance Criteria (Epic)
- Visual workflows versioned and deployable; approvals enforced by risk; rollback captured for med/high risk.
- Releases aggregate related records; gates block promotions until readiness conditions satisfied; notifications sent on key transitions.

## Story Seeds
1) Flowable workflow builder integration and model deployment.
2) Risk-based approval policy service with CAB.
3) Change calendar with conflict detection and overrides.
4) Release assembly, stage gate evaluation and promotion.
5) Deployment tracking with status and audit trail.

## Verification & Done
- Gate enforcement tests; approval policy tests; workflow deployment smoke tests.
- Audit trail completeness verified; release KPIs computed.

