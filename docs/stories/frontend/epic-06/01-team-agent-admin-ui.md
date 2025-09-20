Status: Draft

# Story
As an administrator,
I want a Team & Agent admin UI for hierarchy, skills, and capacity,
so that routing inputs are accurate and up to date.

## Acceptance Criteria
1. Views to manage org→dept→team hierarchy; add/remove agents; edit agent profiles (timezone, certifications).
2. Skill matrix editor per agent with levels; validations and bulk edit; audit preview of changes.
3. Capacity editor per team/agent (current load, caps) with capacity.updated reflected in UI.
4. Role‑based access: admin‑only edits; others read‑only; changes show who/when.
5. Performance and accessibility: list p95 < 800ms; a11y AA.

## Tasks / Subtasks
- [ ] Hierarchy views and membership management (AC: 1)
- [ ] Skill matrix editor with levels and validation (AC: 2)
- [ ] Capacity editor and live updates (AC: 3)
- [ ] RBAC UI states and audit preview (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-06/01-teams-agents-skills-capacity-data-model.md
- PRD: 4.x team structure and skills
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway; 0002 user module
- APIs: teams.yaml, users.yaml

## Testing
- Hierarchy/skill/capacity flows; RBAC states; a11y/perf

## Change Log
| Date       | Version | Description                               | Author |
|------------|---------|-------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — team/agent admin UI       | PO     |

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

