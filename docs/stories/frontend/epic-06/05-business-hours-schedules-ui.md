Status: Draft

# Story
As a workforce planner,
I want a business hours and holiday schedules UI per team/agent,
so that availability is correct for routing and reporting.

## Acceptance Criteria
1. Create/edit schedules with timezone, weekday intervals, exceptions, and holiday calendars; validations prevent overlap.
2. Calendar preview visualizes active hours and holidays; timezone switch previews local vs team time.
3. Bulk apply schedules to agents; audit who/when; RBAC for admin‑only edits.
4. Accessibility and performance standards met; list p95 < 800ms.
5. Changes propagate to backend; availability previews reflect new schedules.

## Tasks / Subtasks
- [ ] Schedule editor with validations and holiday management (AC: 1)
- [ ] Calendar preview and timezone switch (AC: 2)
- [ ] Bulk apply and audit (AC: 3)
- [ ] a11y/perf checks; availability preview (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-06/05-business-hours-timezone-availability.md
- PRD: timezone‑aware routing; business hours
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: teams.yaml, users.yaml

## Testing
- Overlap validation; holiday CRUD; preview accuracy; RBAC; performance

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — business hours schedules UI   | PO     |

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

