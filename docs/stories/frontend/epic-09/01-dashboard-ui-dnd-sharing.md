Status: Draft

# Story
As a user,
I want a drag‑and‑drop dashboard UI with sharing,
so that I can compose and share views of key metrics.

## Acceptance Criteria
1. Drag‑and‑drop grid to add/move/resize widgets; layout persists with optimistic updates and conflict handling.
2. Share controls: private, shared with users/teams, or public link (if policy allows); visibility indicators on dashboard.
3. Role‑based visibility on widgets respected; unauthorized data never rendered; error state shown if blocked.
4. Keyboard accessibility for move/resize; responsive layout; a11y AA.
5. Performance: layout operations responsive; dashboard load p95 < 1s with 12 widgets.

## Tasks / Subtasks
- [ ] DnD grid and layout persistence with optimistic concurrency (AC: 1)
- [ ] Share controls UI and visibility indicators (AC: 2)
- [ ] Widget container enforces RBAC and error states (AC: 3)
- [ ] a11y/responsive + keyboard controls (AC: 4)
- [ ] Performance budget and lazy loading (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-09/01-dashboard-model-widget-framework.md
- PRD: 2.10 Dashboards
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (dashboards/widgets)

## Testing
- DnD operations; conflict behavior; sharing and visibility; a11y; performance

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Dashboard UI DnD & sharing   | PO     |

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

