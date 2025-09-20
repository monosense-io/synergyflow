Status: Draft

# Story
As a data steward,
I want a lineage browser dashboard,
so that I can explore dataset dependencies and diagnose data issues.

## Acceptance Criteria
1. Visualize lineage graph (datasets, sources, sinks) with pan/zoom, expand/collapse, and search; show time context (last refresh).
2. Filters by domain/owner/status; highlight upstream/downstream from a selected dataset; show transformation notes.
3. Snapshot time‑travel: view lineage at a past point using lineage records; compare to current.
4. Export lineage as JSON/PNG; permissions enforced.
5. Accessibility and performance: render medium graphs smoothly; keyboard navigation.

## Tasks / Subtasks
- [ ] Graph rendering and interactions (AC: 1)
- [ ] Filters and highlight upstream/downstream (AC: 2)
- [ ] Time‑travel lineage view and compare (AC: 3)
- [ ] Export and permissions (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-10/03-refresh-scheduler-lineage-tracker.md
- PRD: 2.12 lineage transparency
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (lineage)

## Testing
- Graph correctness; time‑travel behavior; export; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — lineage browser dashboard    | PO     |

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

