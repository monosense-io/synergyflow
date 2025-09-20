Status: Draft

# Story
As a service portfolio manager,
I want a service metrics dashboard,
so that I can monitor volume, fulfillment time, SLA attainment, and CSAT by service.

## Acceptance Criteria
1. Dashboard shows KPIs per service with filters (date range, team) and comparisons (period over period).
2. Widgets: volume trend, fulfillment time distribution, SLA attainment, CSAT; each links to underlying data.
3. Export charts/data (PNG/CSV) with watermark for sensitive reports; respects permissions.
4. Performance: filter changes update widgets p95 < 1s with caching.
5. Accessibility and responsive layout.

## Tasks / Subtasks
- [ ] KPI widgets and filters (AC: 1, 2)
- [ ] Export (PNG/CSV) with permissions (AC: 3)
- [ ] Caching and performance tuning (AC: 4)
- [ ] a11y and responsive checks (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-04/05-service-performance-metrics-automation.md
- PRD: 2.6 metrics
- Architecture: data-architecture.md; gateway-envoy.md
- ADRs: 0011 cache store; 0007 gateway
- APIs: system.yaml (metrics endpoints)

## Testing
- KPI correctness vs sample data; export functions; permission checks
- Performance and a11y tests

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — metrics dashboard (04)       | PO     |

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

