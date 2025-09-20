Status: Draft

# Story
As a catalog administrator,
I want a Service admin UI to create, publish, and deprecate services/offerings,
so that I can manage the catalog with governance and clarity.

## Acceptance Criteria
1. List view with filters (category, owner, status) and pagination; shows version, visibility, and updated date.
2. Create/Edit forms with validation (required fields, SLA format); publish/deprecate actions with confirmation and rationale.
3. Deactivated/Deprecated services clearly labeled; hidden from non-admin UIs.
4. Audit preview shows recent changes for a service.
5. Accessibility and performance: list p95 < 800ms; form interactions responsive.

## Tasks / Subtasks
- [ ] List/filter/sort with pagination (AC: 1, 5)
- [ ] Create/Edit form with validations; publish/deprecate flows (AC: 2)
- [ ] Status/visibility indicators and admin-only controls (AC: 3)
- [ ] Audit preview panel (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-04/01-service-entity-crud-lifecycle.md
- PRD: 2.6 Service Catalog
- Architecture/UX: docs/ux/README.md; gateway-envoy.md for admin routes
- ADRs: 0007 gateway
- APIs: service-requests.yaml (admin endpoints)

## Testing
- UI filters, form validation, lifecycle actions, and role-based visibility
- a11y checks; performance timings

## Change Log
| Date       | Version | Description                                | Author |
|------------|---------|--------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Service admin UI (04)      | PO     |

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

