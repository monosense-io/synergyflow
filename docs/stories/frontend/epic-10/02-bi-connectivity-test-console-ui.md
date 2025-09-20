Status: Draft

# Story
As a BI administrator,
I want a BI connectivity test console UI,
so that I can validate access, security, and performance for BI tools.

## Acceptance Criteria
1. Select a connectivity profile (tool) and dataset; input credentials if needed; run test to fetch sample rows.
2. Results show row/field‑level security effects (masked/redacted fields) and performance metrics; pass/fail summary.
3. Download connection instructions for the selected tool/profile; copy connection strings; scoped to permissions.
4. Audit preview shows recent connection tests; RBAC restricts who can run tests.
5. Accessibility and performance: test UX responsive; a11y AA.

## Tasks / Subtasks
- [ ] Profile/dataset selection and credential input (AC: 1)
- [ ] Run test and display masked results + metrics (AC: 2)
- [ ] Instructions download and copy helpers (AC: 3)
- [ ] Audit preview and RBAC states (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-10/02-bi-connectivity-rowlevel-security.md
- PRD: 2.12 BI connectivity
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (bi connectivity tests)

## Testing
- Test flows across profiles; masking checks; performance
- RBAC; a11y; instruction content

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — BI connectivity test console | PO     |

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

