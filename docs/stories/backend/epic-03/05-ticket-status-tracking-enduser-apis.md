Status: Draft

# Story
As an end user,
I want to track the status and history of my submitted tickets,
so that I stay informed and can follow up when needed.

## Acceptance Criteria
1. API to list a user’s tickets (requests and/or incidents) with filters (status, date range, service) and pagination.
2. Detail endpoint returns history timeline with communications, status changes, attachments; hides internal agent-only notes.
3. Real-time updates via events to notify the portal of changes (request.updated / incident.updated).
4. Access controls ensure users only see their own tickets; admins can view with audit trail.
5. p95 list/detail latency < 600ms under nominal load.

## Tasks / Subtasks
- [ ] List endpoint with filters/pagination (AC: 1)
- [ ] Detail timeline endpoint with redaction for internal notes (AC: 2)
- [ ] Event hooks and subscriptions for updates (AC: 3)
- [ ] Access control enforcement and auditing (AC: 4)
- [ ] Performance tuning (AC: 5)

## Dev Notes
- PRD: 2.5 Portal (track tickets; view history and communications)
- ADRs: 0001 event bus; 0007 gateway
- Architecture: security-architecture.md (access controls); data-architecture.md
- APIs: service-requests.yaml, incidents.yaml, events.yaml

## Testing
- Filter and pagination correctness; redaction correctness
- Event update propagation tests
- Authorization tests; performance checks

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — status tracking (Epic 03)    | PO     |

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

