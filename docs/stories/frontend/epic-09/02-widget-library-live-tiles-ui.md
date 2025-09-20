Status: Draft

# Story
As a user,
I want a widget library with live tiles,
so that I can add real‑time metrics to dashboards with minimal setup.

## Acceptance Criteria
1. Widget library lists available types with previews and configuration forms (data source, filters, refresh interval).
2. Live tiles auto‑refresh via SSE/WebSocket; fallback to polling with ETags; status indicator shows connection state.
3. Tiles include counts, trends, SLA risk, backlog breakdown; each renders accessible charts/tables.
4. a11y AA compliance for charts with text alternatives; responsive rendering.
5. Performance: initial tile render p95 < 500ms after data fetch; refresh under 1s from event.

## Tasks / Subtasks
- [ ] Widget library UI and config forms (AC: 1)
- [ ] Live update wiring (SSE/WebSocket + polling fallback) (AC: 2)
- [ ] Implement tile renderers and a11y alternatives (AC: 3, 4)
- [ ] Performance profiling and budgets (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-09/02-live-tiles-event-streams.md
- PRD: 2.10 live tiles
- Architecture/UX: docs/ux/README.md; gateway-envoy.md for streaming policies
- ADRs: 0007 gateway
- APIs: system.yaml (tiles)

## Testing
- Config flows; live updates; a11y checks; performance timings

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Widget library & live tiles  | PO     |

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

