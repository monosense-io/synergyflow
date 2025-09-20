Status: Draft

# Story
As an end user,
I want to browse the service catalog and request services with clarity on SLAs,
so that I can choose the right offering and know expectations.

## Acceptance Criteria
1. Catalog UI shows services/offerings with owner, category, SLA summary; filters and search available.
2. Detail view shows description, SLA, and the dynamic request form preview; starts request flow.
3. Visibility rules respected: deactivated services hidden for non-admins.
4. Accessibility and responsive layout; keyboard navigation functional.
5. Performance: catalog listing p95 < 800ms after filter changes with pagination.

## Tasks / Subtasks
- [ ] Catalog list with filters/search and pagination (AC: 1, 5)
- [ ] Detail page with SLA and form preview; start request (AC: 2)
- [ ] Visibility handling and error states (AC: 3)
- [ ] a11y/responsive checks (AC: 4)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-03/04-service-catalog-browse-request-sla.md
- PRD: 2.5 Portal — catalog browsing, SLAs
- Architecture/UX: docs/ux/README.md; gateway policies
- APIs: service-requests.yaml

## Testing
- Filter/search behavior; pagination
- Detail data correctness and request initiation
- a11y and performance checks

## Change Log
| Date       | Version | Description                                    | Author |
|------------|---------|------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — catalog UI (Epic 03)          | PO     |

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

