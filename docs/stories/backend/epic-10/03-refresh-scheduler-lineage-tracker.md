Status: Draft

# Story
As a data operations engineer,
I want dataset refresh scheduling and lineage tracking,
so that data freshness is managed and lineage is transparent for governance.

## Acceptance Criteria
1. RefreshSchedule defines cron/interval schedules, timezones, dependencies, and failure policies; supports on‑demand refresh.
2. Scheduler runs refresh jobs with concurrency limits and retries; events: dataset.refresh.requested/completed with status and latency.
3. LineageRecord tracks dataset inputs/outputs and transformations; lineage API exposes upstream/downstream graph and time‑travel queries.
4. UI/API to inspect lineage and refresh status; alerts on failures via Notifications; metrics exposed for freshness SLOs.
5. Audit of schedule changes and refresh runs; permission controls on who can schedule/trigger refresh.

## Tasks / Subtasks
- [ ] RefreshSchedule model and APIs with TZ handling (AC: 1)
- [ ] Scheduler engine with concurrency/retries and events (AC: 2)
- [ ] LineageRecord storage and graph queries (AC: 3)
- [ ] Status/metrics endpoints; notification hooks (AC: 4)
- [ ] Audit and permissions (AC: 5)

## Dev Notes
- PRD: 2.12 refresh scheduling and lineage
- Architecture: data-architecture.md; observability-reliability.md
- ADRs: 0006 event partitioning; 0001 event bus; 0011 cache (optional)
- APIs: system.yaml (schedules/status/lineage)

## Testing
- Schedule execution and retry logic; events
- Lineage graph correctness and time‑travel queries
- Metrics and alerting behavior; permission tests

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — refresh scheduler & lineage        | PO     |

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

