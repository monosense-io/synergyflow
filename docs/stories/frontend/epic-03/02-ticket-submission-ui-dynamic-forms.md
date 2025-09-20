Status: Draft

# Story
As an end user,
I want a guided ticket submission UI with dynamic forms and attachments,
so that I can submit accurate requests/incidents easily.

## Acceptance Criteria
1. Form renderer fetches schema for selected service/type and dynamically shows/hides fields with validation.
2. Attachments can be added/removed with size/type validation and upload progress.
3. On submit, validation errors are displayed per field; successful submission shows ticket ID and SLA.
4. Accessibility: keyboard navigation for form fields, proper labels/aria attributes, error summaries.
5. Performance: first interaction to render form p95 < 500ms after schema fetch.

## Tasks / Subtasks
- [ ] Dynamic form renderer and schema fetching (AC: 1)
- [ ] Conditional fields and validation messages (AC: 1, 3)
- [ ] Attachment uploader with progress and limits (AC: 2)
- [ ] Submission handling and success/error UX (AC: 3)
- [ ] a11y checks and performance measures (AC: 4, 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-03/02-guided-ticket-submission-dynamic-forms.md
- PRD: 2.5 Portal — guided forms, attachments, SLA
- Architecture: UX standards docs/ux/README.md; gateway-envoy.md for upload policy
- ADRs: 0007 gateway; 0008 push policy if notifications used
- APIs: service-requests.yaml (or incidents.yaml)

## Testing
- Form logic/validation tests; attachment flows; submission success/failure
- a11y tests; performance timing with schema size variations

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — submission UI (Epic 03)       | PO     |

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

