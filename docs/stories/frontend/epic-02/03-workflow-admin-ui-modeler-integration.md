Status: Draft

# Story
As a change administrator,
I want a workflow administration UI with Flowable Modeler integration,
so that I can author, validate, version, and deploy change workflows.

## Acceptance Criteria
1. List workflows and versions with metadata; view diff between versions.
2. Open/Edit workflow using embedded Flowable Modeler (or external link) and save back to repository.
3. Validate workflow and display errors/warnings prior to deployment.
4. Deploy selected version; show deployment history and allow rollback (with confirmation and rationale).
5. RBAC enforcement: only admins can create/edit/deploy; audit user actions.

## Tasks / Subtasks
- [ ] Workflows list/detail views with version timeline (AC: 1)
- [ ] Modeler integration (embed or link) with save hooks (AC: 2)
- [ ] Validation UI showing errors/warnings (AC: 3)
- [ ] Deployment/rollback UI with history and rationale (AC: 4)
- [ ] RBAC checks; audit trail capture in UI (AC: 5)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-02/01-flowable-workflow-builder-integration-model-deployment.md
- PRD 2.3 visual workflow builder and deployment
- ADRs: 0012 Flowable orchestration; 0007 gateway; 0004 event versioning
- Architecture: security-architecture.md for RBAC; gateway-envoy.md policies
- APIs: changes.yaml workflow endpoints; system.yaml validation

## Testing
- UI CRUD/versioning flows; diff rendering
- Modeler save/validation/deploy/rollback flows
- RBAC and audit behavior in UI

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — workflow admin UI for Epic 02 | PO   |

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

