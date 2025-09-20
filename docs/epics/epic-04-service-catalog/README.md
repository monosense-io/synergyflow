---
id: epic-04-service-catalog
title: Epic 04 — Service Catalog & Fulfillment
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 04 — Service Catalog & Fulfillment

## Epic Goal
Provide a comprehensive catalog of services with dynamic request forms, multi-stage approvals, and automated/manual fulfillment (PRD 2.6).

## Scope
- In scope: Service definitions, dynamic forms, per-service approvals, fulfillment workflows, metrics and automation for standard requests.
- Out of scope: Portal UX (Epic 03) beyond required integration points.

## Key Requirements
- PRD 2.6: Create/edit/deactivate services; conditional form fields; multi-stage approvals; fulfillment tasks with audit and notifications; performance metrics.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Workflow & task orchestration: docs/architecture/workflow-flowable.md, docs/adrs/0012-workflow-orchestration-flowable-7.md
- Event-driven comms: docs/adrs/0001-event-only-inter-module-communication.md

## API & Events Impact
- APIs: docs/api/modules/service-requests.yaml, docs/api/modules/users.yaml
- Events: service.created/updated, request.submitted, request.approved, fulfillment.task.completed

## Data Model Impact
- Entities: Service, Offering, RequestFormSchema, ApprovalFlow, FulfillmentTask, SLA.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 03 (Portal), Epic 11 (Notifications), Epic 12 (Security & RBAC), Epic 09 (Reports) for metrics.

## Risks & Mitigation
- Form schema complexity → JSON schema with validator; schema versioning.
- Approval deadlocks → timeouts and delegation; escalation rules.

## Acceptance Criteria (Epic)
- Services manageable through API; dynamic forms validated; per-service approvals enforced; fulfillment tasks tracked with audit.

## Story Seeds
1) Service entity + CRUD + lifecycle.
2) Dynamic form schema engine with conditional logic.
3) Approval flow editor and executor.
4) Fulfillment workflow integration and audit trail.

## Verification & Done
- Form/approval executors tested; audit completeness verified; metrics available.

