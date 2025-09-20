Status: Draft

# Story
As a reliability analyst,
I want delivery metrics APIs and audit reporting,
so that notification health is measured and issues can be diagnosed.

## Acceptance Criteria
1. Metrics computed per channel and topic: delivery latency p50/p95, failure rates, retry counts, dedupe/grouping rates, digest counts.
2. APIs expose metrics and time series with filters (channel, topic, timeframe), plus top failure reasons; export CSV/JSON.
3. Audit reporting API returns delivery histories with filters (user, channel, status, time range) and redacts PII per policy.
4. Alerts on failure rate/latency thresholds integrate with Epic 09 dashboards and Epic 13 reliability runbooks.
5. Data retention policies applied to delivery/audit records; configurable TTLs and archival.

## Tasks / Subtasks
- [ ] Metrics aggregation and storage (AC: 1)
- [ ] Query/export APIs and top‑K reason analysis (AC: 2)
- [ ] Audit reporting API with PII redaction (AC: 3)
- [ ] Alert hooks and dashboard integration (AC: 4)
- [ ] Retention policy jobs and configs (AC: 5)

## Dev Notes
- PRD: docs/prd/07-notification-system-design.md (operational monitoring)
- Dependencies: Epic 09 dashboards; Epic 13 reliability
- ADRs: 0011 cache (for dashboard tiles); 0001 event bus
- Architecture: observability-reliability.md; data-architecture.md
- APIs: system.yaml (metrics/audit)

## Testing
- Metric correctness; top‑K analysis; export
- Audit PII redaction; retention enforcement
- Alert threshold triggers and dashboard integration

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — delivery metrics & audit        | PO     |

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

