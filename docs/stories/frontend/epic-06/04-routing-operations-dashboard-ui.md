Status: Draft

# Story
As a NOC engineer,
I want a routing operations dashboard showing queues, capacity, SLA risk, and escalations,
so that I can detect issues and intervene quickly.

## Acceptance Criteria
1. Live widgets: current queue by category/priority, team capacity utilization, routing latency p95, SLA risk forecast, recent escalations.
2. Filters: team, service, timeframe; drill‑down to tickets/teams; export CSV/PNG.
3. Alerts panel shows anomalies (e.g., capacity spikes, routing latency breaches); link to remediation actions.
4. Accessibility and performance: widgets refresh under 2s; a11y AA.
5. Real‑time updates via SSE/WebSocket; polling fallback.

## Tasks / Subtasks
- [ ] Widgets and filters with drill‑down (AC: 1, 2)
- [ ] Alerts panel and remediation links (AC: 3)
- [ ] Real‑time updates/polling; export (AC: 4, 5)
- [ ] a11y/perf checks (AC: 4)

## Dev Notes
- Backend: docs/stories/backend/epic-06/04-telemetry-feedback-loop-sla-csat.md and routing API metrics
- PRD: SLA optimization and operational monitoring
- Architecture: observability-reliability.md; gateway-envoy.md
- ADRs: 0011 cache for dashboards; 0007 gateway
- APIs: system.yaml (metrics), teams.yaml

## Testing
- Widget accuracy; real‑time updates; export; a11y/perf

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — routing operations dashboard    | PO     |

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

