Status: Draft

# Story
As a workforce planner,
I want business hours and timezone-aware availability computation,
so that routing respects local schedules, holidays, and staffing plans.

## Acceptance Criteria
1. Define BusinessHours schedules per team and agent with timezone, weekly hours, and holiday calendars.
2. Availability API returns whether an agent/team is available now and the next availability window; supports timezone conversion and DST.
3. Routing integration hook consumes availability status; capacity caps can be time‑bounded (e.g., shifts).
4. Admin APIs to CRUD schedules and holidays; validations prevent overlapping or invalid intervals.
5. Performance: availability computation p95 < 50ms per agent with caching.

## Tasks / Subtasks
- [ ] BusinessHours + Holiday models and migrations (AC: 1)
- [ ] Availability computation service with TZ/DST handling (AC: 2)
- [ ] Routing integration hook for availability (AC: 3)
- [ ] Admin APIs with validations (AC: 4)
- [ ] Caching for hot paths and performance tests (AC: 5)

## Dev Notes
- PRD: docs/prd/04-multi-team-support-intelligent-routing.md (business hours, timezone‑aware routing)
- ADRs: 0006 event partitioning; 0001 event bus
- Architecture: data-architecture.md; api-architecture.md
- APIs: teams.yaml, users.yaml

## Testing
- TZ/DST edge cases; overlapping intervals validation
- Availability correctness across timezones
- Performance tests for p95 target

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — business hours & availability   | PO     |

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

