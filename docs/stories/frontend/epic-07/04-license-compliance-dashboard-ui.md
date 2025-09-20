Status: Draft

# Story
As a software asset manager,
I want a license compliance dashboard,
so that I can monitor usage vs entitlements and act before breaches.

## Acceptance Criteria
1. KPIs per product: seats, usage, remaining, threshold status; filters by product/team/timeframe.
2. Alerts list for approaching/breached thresholds with owners and actions (assign, reclaim, contact).
3. Export CSV/PNG; respects permissions; watermark for sensitive exports.
4. Performance: filter changes update widgets p95 < 1s; caching applied.
5. Accessibility and responsive layout.

## Tasks / Subtasks
- [ ] KPI widgets and filters; alerts list (AC: 1, 2)
- [ ] Actions (assign/reclaim/contact) flows (AC: 2)
- [ ] Export and permissions (AC: 3)
- [ ] Caching/performance; a11y/responsive checks (AC: 4, 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-07/03-license-monitoring-compliance-alerts.md
- PRD: 2.8 license monitoring and alerts
- Architecture: data-architecture.md; gateway-envoy.md
- ADRs: 0011 cache store; 0007 gateway
- APIs: system.yaml (license reports)

## Testing
- KPI accuracy; alerts behavior; actions; exports; performance and a11y

## Change Log
| Date       | Version | Description                                         | Author |
|------------|---------|-----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — license compliance dashboard (07)   | PO     |

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

