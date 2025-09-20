Status: Draft

# Story
As a knowledge engineer,
I want a rich article editor UI with versioning, attachments, and rollback,
so that I can author and maintain high-quality content.

## Acceptance Criteria
1. Rich text editor supports headings, lists, links, code blocks, images/attachments with drag & drop and size/type validation.
2. Version history view shows diffs between versions; rollback action requires confirmation and rationale.
3. Draft→Review submit flow with validation; shows assigned reviewers; error messages surfaced from backend.
4. Attachment manager lists files per version; secure download links; remove/replace in Draft.
5. Accessibility and performance: editor interactions responsive; a11y AA compliance.

## Tasks / Subtasks
- [ ] Editor component and attachment upload; validations (AC: 1)
- [ ] Version history and diff viewer; rollback UI (AC: 2)
- [ ] Submit for review; error handling; reviewer display (AC: 3)
- [ ] Attachment manager with secure links (AC: 4)
- [ ] a11y/performance checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-05/01-knowledge-article-model-versioning-attachments.md
- PRD: 2.7 Knowledge Management — versioning, attachments, approvals
- Architecture/UX: docs/ux/README.md; gateway-envoy.md for upload policy
- ADRs: 0007 gateway (admin routes)
- APIs: knowledge.yaml

## Testing
- Editor formatting, uploads, and validations
- Version diff/rollback flows; review submission
- a11y audits; performance timings

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — article editor UI (Epic 05)      | PO     |

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

