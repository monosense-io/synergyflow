# Project-Type Questionnaire — Enterprise ITSM + PM

Date: 2025-10-06
Owner: Architect
Scope: Domain-specific decisions for ITSM/PM projects

## ITSM (Incident/Request)
- ITIL Alignment: Incident vs Service Request modeled distinctly; approval required for configured request types.
- SLA Policy: Priority-based (Critical 2h, High 4h, Medium 8h, Low 24h); business-calendars and pauses supported.
- Routing: Rules engine (category/priority/round-robin) with audit and override.
- Audit: Full ticket history, comments, state transitions retained 7 years.

## PM (Issues/Boards/Sprints)
- Workflow: Kanban with configurable columns, WIP limits, and state guards.
- Capacity: Sprint capacity validation prevents over-allocation.
- Contracts: API work guarded by contract-first flow (OpenAPI PR gate).

## Shared Workflow (Approvals)
- Scope: Single-level approvals in MVP; bundles and simulation planned.
- AuthZ: Batch authorization for composite reads; RBAC enforced server-side.

## Data & Compliance
- PII Handling: Minimize exposure in read models; internal notes masked by role.
- Retention: Audit 7 years; operational data per legal/compliance.

## Observability & SLOs
- SLOs: Queue <200ms p95; side-panel <300ms p95; CRUD <400ms p95; event E2E ≤2s p95.
- Signals: Outbox/stream lag, timer drift, SSE reconnects, reducer stale-drops.

## Notes
- This Q/A informs architecture sections and acceptance gates; revisit each phase.

