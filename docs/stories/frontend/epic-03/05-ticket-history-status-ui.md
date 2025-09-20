Status: Draft

# Story
As an end user,
I want to view my ticket history and current statuses with updates,
so that I can track progress and respond when needed.

## Acceptance Criteria
1. List view of my tickets with filters (status, date range, service); shows SLA status badge.
2. Detail view shows timeline of status changes, communications, and attachments; internal notes are not shown.
3. Real-time updates for status changes via SSE/WebSocket or polling fallback with exponential backoff.
4. Accessibility and responsive layout.
5. Performance: list p95 < 800ms; detail initial render p95 < 600ms after data fetch.

## Tasks / Subtasks
- [ ] Tickets list with filters and badges (AC: 1, 5)
- [ ] Detail timeline with communications/attachments (AC: 2)
- [ ] Real-time updates or efficient polling (AC: 3)
- [ ] a11y/responsive checks (AC: 4)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-03/05-ticket-status-tracking-enduser-apis.md
- PRD: 2.5 Portal — track tickets; history and communications
- Architecture: gateway-envoy.md policies; UX standards docs/ux/README.md
- ADRs: 0007 gateway; 0008 push (if used)
- APIs: service-requests.yaml, incidents.yaml

## Testing
- Filter and performance tests
- Real-time updates/polling behavior; timeline rendering
- a11y checks

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — ticket history/status UI (03)  | PO     |

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

