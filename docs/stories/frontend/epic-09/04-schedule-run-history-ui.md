Status: Draft

# Story
As a reporting admin,
I want a schedules and run history UI,
so that I can manage scheduled reports and monitor executions.

## Acceptance Criteria
1. Create/edit schedules with cron/interval, timezone, recipients, and format; validations and preview of next runs.
2. Permissions: only authorized users can create schedules and target recipients; UI hides unauthorized actions.
3. Run history view with filters (report, timeframe, status); shows duration, row counts, and download links.
4. Email delivery status visible; failed runs show error summaries; re‑run action available with safety checks.
5. Accessibility and performance standards met; history pagination responsive.

## Tasks / Subtasks
- [ ] Schedule editor and validations with next‑run preview (AC: 1)
- [ ] Permissions UX and recipient picker (AC: 2)
- [ ] History list with filters and details (AC: 3)
- [ ] Delivery status and re‑run actions (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-09/04-report-scheduler-email-delivery.md
- PRD: 2.11 scheduling and distribution
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (schedules/history)

## Testing
- Schedule creation/validation; permissions; history filters; re‑run; a11y/perf

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — schedules & history UI      | PO     |

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

