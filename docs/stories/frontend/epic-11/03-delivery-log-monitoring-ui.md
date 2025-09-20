Status: Draft

# Story
As a reliability analyst,
I want a delivery log and monitoring UI,
so that I can search deliveries, diagnose failures, and track health.

## Acceptance Criteria
1. Delivery list with filters (channel, topic, status, timeframe, user) and pagination; shows status history and provider IDs.
2. Detail view shows payload preview (PII‑redacted), retries, DLQ status, and error summaries; actions to retry (admin‑only) when safe.
3. KPI widgets: latency p95, failure rates, retry counts; link to Epic 09 dashboards.
4. Export CSV of filtered results with permissions; audit of UI actions (retry, export).
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Delivery list/filters and detail view with redaction (AC: 1, 2)
- [ ] Retry action with confirmation and safety checks (AC: 2)
- [ ] KPI widgets and links to dashboards (AC: 3)
- [ ] Export and audit (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-11/03-channel-adapters-email-push-inapp.md and 05‑delivery‑metrics‑audit‑reporting.md
- PRD: 07 notifications — reliability and monitoring
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (delivery search, metrics)

## Testing
- Filters; redaction; retry flow; KPIs; export; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — delivery log & monitoring UI | PO     |

## Dev Agent Record

### Agent Model Used
<record at implementation time>

### Debug Log References
<links at implementation time>

### Completion Notes List
<notes at implementation time>

### File List
<files at implementation time>

## QA Results
<QA to fill>

