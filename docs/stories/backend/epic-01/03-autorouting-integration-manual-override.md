Status: Draft

# Story
As a service desk supervisor,
I want auto-routing integration with manual override,
so that incidents are assigned to the best agent while retaining human control.

## Acceptance Criteria
1. On incident creation/update, a routing request is sent to the Routing service with necessary context (category, skills, SLA, capacity).
2. On routing decision, the incident is assigned and ticket.routed event is emitted; decision is audit logged with rationale.
3. Manual override allows reassignment with required rationale and audit trail.
4. Routing failures fall back to a safe default queue with notification.
5. Latency p95 for routing decision ≤ 2s under normal load.

## Tasks / Subtasks
- [ ] Define routing request/response contracts (AC: 1)
- [ ] Integration with Routing service (Epic 06) and event handling (AC: 1, 2)
- [ ] Assignment application and audit log with rationale (AC: 2)
- [ ] Manual override endpoint with policy checks (AC: 3)
- [ ] Fallback queue and notification path (AC: 4)
- [ ] Performance tests for p95 latency (AC: 5)

## Dev Notes
- PRD: 2.1 Incident Management (auto-routing) and 4.x Multi-Team & Routing
- Dependencies: Epic 06 routing engine (docs/epics/epic-06-routing-multiteam/README.md), Epic 11 Notifications
- ADRs: 0001 event bus, 0006 partitioning, 0002 shared user module
- APIs: incidents.yaml, teams.yaml, users.yaml, events.yaml

## Testing
- Contract tests for routing request/response
- Audit logging tests including manual override rationale
- Fallback behavior and notification tests
- Performance tests targeting p95 ≤ 2s

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 01 seed #3           | PO     |

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

