Status: Draft

# Story
As a reliability lead,
I want a reliability reviews UI,
so that I can review SLO performance, incidents, drills, and remediation items in one place.

## Acceptance Criteria
1. Create/view ReliabilityReview records summarizing period SLOs, breaches, top incidents, drills, and action items.
2. Assign owners/dates to remediation tasks; track status; link to tickets.
3. Export review packet (PDF/ZIP) with charts and evidence links; permissions enforced.
4. Accessibility and performance standards met; a11y AA.
5. Notifications to stakeholders on review publication.

## Tasks / Subtasks
- [ ] Review creation and summary composer (AC: 1)
- [ ] Remediation task assignment/tracking (AC: 2)
- [ ] Export packet and permissions (AC: 3)
- [ ] a11y/perf checks; notifications (AC: 4, 5)

## Dev Notes
- Backend: SLO/Review APIs from backend stories 01 and 03
- PRD: reliability reviews and runbooks
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0011 cache; 0007 gateway
- APIs: system.yaml (reviews)

## Testing
- Summary accuracy; export; permissions; notifications; a11y/perf

## Change Log
| Date       | Version | Description                           | Author |
|------------|---------|---------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — reliability reviews UI| PO     |

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

