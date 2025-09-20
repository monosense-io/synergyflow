Status: Draft

# Story
As a problem management analyst,
I want trend reporting for recurring categories and services,
so that we can prioritize problem investigations and improvements.

## Acceptance Criteria
1. Generate trend reports identifying top recurring incident categories/services over configurable periods.
2. Filters include date range, team, service, severity; export to CSV.
3. Report generation time p95 ≤ 5s on 90-day data set; results cached.
4. API supports pagination and sorting; permissions enforced.
5. Audit logs record report executions with parameters.

## Tasks / Subtasks
- [ ] Reporting queries with guardrails and indexes (AC: 1, 3)
- [ ] Filters, pagination, sorting in API (AC: 2, 4)
- [ ] Caching layer with TTL and invalidation (AC: 3)
- [ ] CSV export endpoint (AC: 2)
- [ ] Audit logging for report runs (AC: 5)

## Dev Notes
- PRD 2.2 Problem Management — trend reports for recurring issues
- Consider alignment with PRD 2.11 (reports) but scope this story to backend data services
- ADRs: 0011 cache store (DragonflyDB) for caching; 0006 event partitioning not required here
- APIs: search.yaml or system.yaml for report endpoints
- Data: time-series aggregation by category/service; ensure indexes on (category, service, created_at)

## Testing
- Query performance tests with realistic data volumes
- Filter correctness tests; CSV export correctness
- Permission tests; audit log checks

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 01 seed #6           | PO     |

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

