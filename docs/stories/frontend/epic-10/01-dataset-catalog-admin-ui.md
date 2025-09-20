Status: Draft

# Story
As a data product owner,
I want a dataset catalog admin UI for contracts and publishing,
so that curated datasets are discoverable, governed, and easy to manage.

## Acceptance Criteria
1. Catalog lists datasets with owner, contract version, freshness, and PII flags; filters (domain, owner, status) and pagination.
2. Contract view shows schema (fields, types, PII), SLAs, and change policy; version history and diff; publish/unpublish actions with audit.
3. Export targets shown with sample download (masked as per policy); copy connection info for warehouse tables.
4. RBAC: only authorized roles edit/publish; others read‑only; all actions audited.
5. Accessibility and performance: list p95 < 800ms; editor responsive.

## Tasks / Subtasks
- [ ] Catalog list with filters and pagination (AC: 1)
- [ ] Contract view with history/diff and publish/unpublish (AC: 2)
- [ ] Export sample and connection info (AC: 3)
- [ ] RBAC UI states and audit previews (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-10/01-dataset-contracts-export-services.md
- PRD: 2.12 datasets and governance
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (datasets/contracts)

## Testing
- Filter/search; publish/unpublish flows; history/diff; sample download; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — dataset catalog admin UI     | PO     |

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

