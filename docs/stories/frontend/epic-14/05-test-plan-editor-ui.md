Status: Draft

# Story
As a QA lead,
I want a test plan editor UI with templates and linkage to stories/TraceLinks,
so that teams can define and maintain plans aligned with acceptance criteria.

## Acceptance Criteria
1. Create/edit TestPlans with sections (scope, risks, environments, data, cases) using templates; link to epics/stories and AC IDs.
2. Cases can reference Given‑When‑Then or external specs; link to existing tests by ID/path; missing links flagged.
3. Publish plan to CI for execution selection; status tracking of planned vs executed; export plan (PDF/Markdown).
4. Permissions and audit: changes tracked; only owners and QA admins can publish; a11y AA; performance responsive.
5. Suggestions to add missing cases based on uncovered AC from Traceability reports.

## Tasks / Subtasks
- [ ] TestPlan editor with templates and linking (AC: 1)
- [ ] Case linking to tests and AC; missing link flags (AC: 2)
- [ ] Publish to CI selection; status tracking and export (AC: 3)
- [ ] Permissions/audit; a11y/perf (AC: 4)
- [ ] Suggestions based on coverage gaps (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-14/04-traceability-reporting-service.md and 01 scaffolding
- PRD: testing strategy — planning and traceability
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (test plans)

## Testing
- Editor flows; linking; publish and status; exports; a11y/perf; suggestions

## Change Log
| Date       | Version | Description                          | Author |
|------------|---------|--------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — test plan editor UI  | PO     |

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

