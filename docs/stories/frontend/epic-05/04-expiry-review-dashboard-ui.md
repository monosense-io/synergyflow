Status: Draft

# Story
As a knowledge admin,
I want an expiry review dashboard,
so that I can triage and act on expiring or stale articles efficiently.

## Acceptance Criteria
1. Dashboard lists articles by expiry status (expiring soon, expired) with filters (owner, tag, age); supports bulk actions (assign reviewer, send reminder).
2. Item detail shows last review, owner, versions, and actions: renew (extend), update (create draft), retire with rationale.
3. Notifications can be triggered from UI; digest status visible; respects preferences.
4. Accessibility and performance: list p95 < 800ms; interactions responsive.
5. Export CSV of filtered list for offline review.

## Tasks / Subtasks
- [ ] Dashboard list, filters, bulk actions (AC: 1)
- [ ] Detail actions and rationale capture (AC: 2)
- [ ] Notification triggers and digest state (AC: 3)
- [ ] a11y/perf checks; CSV export (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-05/05-expiry-notification-scheduler.md
- PRD: 2.7 expiry notifications
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: knowledge.yaml; system.yaml (notifications)

## Testing
- Filter/bulk action behavior; action flows and audit
- Export correctness; performance/a11y checks

## Change Log
| Date       | Version | Description                                    | Author |
|------------|---------|------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — expiry review dashboard (05)  | PO     |

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

