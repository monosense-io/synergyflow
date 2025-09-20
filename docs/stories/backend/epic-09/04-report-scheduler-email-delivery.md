Status: Draft

# Story
As a reporting admin,
I want a report scheduler with email delivery and run history,
so that reports are delivered automatically to stakeholders.

## Acceptance Criteria
1. Schedule model supports cron/interval schedules, timezones, recipients (lists/roles), and delivery formats (CSV/XLSX/PDF).
2. Scheduler enqueues report executions respecting guardrails and concurrency limits; retries with backoff on failures.
3. Email delivery integrates with Notifications; attachments included up to size limits; large results provide secure download links.
4. RunHistory records execution metadata (start/end, status, row counts, duration); APIs to list/filter history and view logs.
5. Security: permissions on schedule creation/recipients; signed links; auditing of deliveries.

## Tasks / Subtasks
- [ ] Schedule model and APIs (create/update/list) with TZ handling (AC: 1)
- [ ] Execution queue and concurrency limits + retries (AC: 2)
- [ ] Email delivery + attachment/link policy (AC: 3)
- [ ] RunHistory storage and history APIs (AC: 4)
- [ ] Security and auditing (AC: 5)

## Dev Notes
- PRD: 2.11 Reports — scheduling and distribution
- Dependencies: Epic 11 Notifications; Security policies for recipients
- Architecture: observability-reliability.md; gateway-envoy.md
- ADRs: 0011 cache (optional); 0001 events (report.scheduled.run)
- APIs: system.yaml (schedules, history)

## Testing
- TZ correctness; queueing/retries; email delivery; history queries; permissions/audit

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Report scheduler & delivery   | PO     |

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

