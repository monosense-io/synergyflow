Status: Draft

# Story
As an API product manager,
I want an API client admin UI for managing clients, keys, and rate limits,
so that consumer access is controlled and observable.

## Acceptance Criteria
1. Clients list with filters (owner, scope, status) and pagination; detail view shows contact, scopes, usage, and throttling events.
2. Issue/rotate/revoke keys with copy‑once display; secrets never shown after; last‑used visible; RBAC for admin‑only actions.
3. Assign per‑client RateLimitPolicies (quota/burst) and preview enforcement; warnings for nearing limits; notifications opt‑in.
4. Usage and throttling charts per client; export CSV; permissions enforced; a11y AA.
5. Performance: list p95 < 800ms; key issuance flow responsive.

## Tasks / Subtasks
- [ ] Clients list/detail and filters (AC: 1)
- [ ] Key lifecycle UI (issue/rotate/revoke) with copy‑once (AC: 2)
- [ ] Policy assignment and preview (AC: 3)
- [ ] Usage/throttling charts and export (AC: 4)
- [ ] a11y/perf checks; RBAC states (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-15/05-api-client-keys-rate-limit-policy-management.md
- PRD: 5 integration — client governance
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway; 0010 Envoy
- APIs: system.yaml (clients/keys/policies)

## Testing
- Key flows; policy assignment; charts/export; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                                       | Author |
|------------|---------|---------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — API client admin UI (15)         | PO     |

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

