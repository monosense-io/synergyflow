Status: Draft

# Story
As a security architect,
I want a central authorization service implementing RBAC/ABAC policies,
so that access control is consistent, auditable, and least‑privilege across modules.

## Acceptance Criteria
1. Data models for Role, Permission, Policy (attribute rules), and Assignments (user→roles, team→roles) with effective permission resolution.
2. Policy engine enforces RBAC (role→permission) and ABAC (contextual attributes: resource owner/team, environment, sensitivity) with deny‑by‑default.
3. Authz API: check (isAllowed), explain (why/why not), and list (what can user do?) endpoints; performance p95 < 10ms per check in memory‑warmed path.
4. Policy versioning and attestation: publish/rollback policies with audit; user.role.changed event emitted on assignment changes.
5. Integration SDK for services to call authz; gateway policy plugin to short‑circuit unauthorized requests.

## Tasks / Subtasks
- [ ] Schemas and migrations (Role, Permission, Policy, Assignment) (AC: 1)
- [ ] Policy engine (RBAC + ABAC) with deny‑by‑default and attribute resolvers (AC: 2)
- [ ] Authz API (check/explain/list) and performance tuning (AC: 3)
- [ ] Policy versioning + publish/rollback + audit + events (AC: 4)
- [ ] SDK for services and gateway integration (AC: 5)

## Dev Notes
- PRD: docs/prd/13-security-compliance-framework.md (roles, approvals, access controls)
- Architecture: docs/architecture/security-architecture.md; docs/architecture/gateway-envoy.md (authz at gateway)
- ADRs: 0007 gateway placement; 0010 Envoy Gateway adoption; 0009 DB migrations; 0001 event bus; 0004 contract versioning
- APIs: users.yaml (users/teams), system.yaml (authz endpoints)

## Testing
- Unit tests for RBAC/ABAC policy evaluation; explainability
- Performance tests p95 < 10ms warmed; cold‑start metrics
- Integration tests with gateway plugin and a sample service

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — central authz service (RBAC/ABAC) | PO  |

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

