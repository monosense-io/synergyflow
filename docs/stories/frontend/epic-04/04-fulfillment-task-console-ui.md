Status: Draft

# Story
As a fulfillment coordinator,
I want a task console UI to manage fulfillment tasks,
so that requests are executed efficiently with clear ownership and audit.

## Acceptance Criteria
1. Task list with filters (status, assignee, service, date) and pagination; bulk actions (assign, reassign) for authorized users.
2. Task detail shows steps, attachments, notes, and audit timeline; actions to start/complete/block with rationale.
3. Notifications/overdue indicators; SLA badges on tasks.
4. Accessibility and responsive layout; performance: list p95 < 800ms.
5. Real-time updates for task state changes; fallback polling with backoff.

## Tasks / Subtasks
- [ ] Task list with filters/pagination and bulk actions (AC: 1)
- [ ] Task detail actions and audit timeline (AC: 2)
- [ ] SLA badges and overdue indicators (AC: 3)
- [ ] Real-time updates/polling; a11y/responsive checks (AC: 4, 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-04/04-fulfillment-workflows-audit-trail.md
- PRD: 2.6 fulfillment workflows
- Architecture/UX: docs/ux/README.md; gateway-envoy.md policies
- ADRs: 0007 gateway; 0001 events
- APIs: service-requests.yaml; users.yaml

## Testing
- Filters, bulk actions, and permissions
- Timeline correctness; real-time updates
- a11y and performance

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — fulfillment task console (04)   | PO     |

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

