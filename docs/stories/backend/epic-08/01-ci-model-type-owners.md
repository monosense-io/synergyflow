Status: Draft

# Story
As a CMDB administrator,
I want a CI model with type-specific attributes and owners,
so that configuration items are consistently represented and governed.

## Acceptance Criteria
1. Define CIType with schema for type-specific attributes (required/optional, formats) and validation rules.
2. CI entity supports core fields (name, description, environment, status, owner) and a typed attributes blob validated against CIType schema.
3. CRUD APIs for CI/CIType enforce validation; status transitions audited; soft-delete for retired CIs.
4. Ownership model supports user/team owners; RBAC enforces who can create/update CIs per type.
5. Emit ci.created/ci.updated events with type and owner references; event schemas versioned.

## Tasks / Subtasks
- [ ] Schemas and migrations for CIType and CI (AC: 1, 2)
- [ ] Validation middleware for CI attributes against CIType (AC: 2)
- [ ] CRUD endpoints with RBAC and audit (AC: 3, 4)
- [ ] Event emission for create/update (AC: 5)
- [ ] Soft-delete and status lifecycle (AC: 3)

## Dev Notes
- PRD: docs/prd/02-itil-v4-core-features-specification.md (2.9 CMDB)
- ADRs: 0001 event bus; 0004 event contract versioning; 0009 DB migration policy
- Architecture: data-architecture.md; api-architecture.md; module-architecture.md
- APIs: docs/api/modules/cmdb.yaml; users.yaml/teams.yaml for ownership

## Testing
- CIType schema validation; CI CRUD happy-path and validation failures
- RBAC enforcement and audit immutability
- Event contract tests for ci.created/ci.updated

## Change Log
| Date       | Version | Description                                | Author |
|------------|---------|--------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — CI model & CIType          | PO     |

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

