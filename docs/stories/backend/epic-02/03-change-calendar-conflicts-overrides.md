Status: Draft

# Story
As a release planner,
I want a change calendar with conflict detection and governed overrides,
so that risky scheduling conflicts are prevented or explicitly accepted with audit.

## Acceptance Criteria
1. Maintain blackout windows (global and per-service) and detect scheduling conflicts; configurable rules determine warn vs block.
2. Detect CI/service conflicts by checking CMDB relationships and change windows; display conflict details via API.
3. Allow manual override by authorized roles with a mandatory rationale; all overrides are audit logged.
4. Calendar APIs support list/view/create/update for change windows; time zone aware and DST safe.
5. Notifications are sent on conflicts and overrides to relevant stakeholders.

## Tasks / Subtasks
- [ ] Blackout window model and rules engine (AC: 1)
- [ ] CMDB conflict detection integration (AC: 2)
- [ ] Override endpoint with RBAC and rationale (AC: 3)
- [ ] Calendar APIs w/ TZ handling and tests (AC: 4)
- [ ] Notification hooks for conflict/override (AC: 5)

## Dev Notes
- PRD 2.3 calendar conflict prevention
- ADRs: 0005 CMDB impact handshake; 0001 event bus; 0007 gateway
- Architecture: data-architecture.md for indexing; observability-reliability.md for scheduling reliability
- APIs: changes.yaml (calendar endpoints), cmdb.yaml (impact), system.yaml (notifications)

## Testing
- Conflict detection unit/integration tests (blackout, CI conflicts)
- Override authorization and audit tests
- Time zone/DST correctness tests
- Notification dispatch tests

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 02 seed #3          | PO     |

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

