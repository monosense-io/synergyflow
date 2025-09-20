Status: Draft

# Story
As a security admin,
I want an SSO configuration admin UI,
so that I can configure IdP settings, group mappings, and validate logins safely.

## Acceptance Criteria
1. Forms for OIDC/SAML config (issuer, client IDs, secrets via SecretRef, redirect URIs) with validation and helpful errors.
2. Group claim→role mapping editor with test input; preview of resulting role assignments.
3. Test login workflow in a sandbox; shows token claims and mapped roles; no changes persisted unless confirmed.
4. RBAC: admin‑only access; audit of changes; secrets never displayed; masked fields with copy‑once download for client secrets.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Config forms and validations; SecretRef handling (AC: 1, 4)
- [ ] Group mapping editor with preview (AC: 2)
- [ ] Test login sandbox (AC: 3)
- [ ] RBAC/audit UI and masking (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-12/02-sso-configuration-session-management.md
- PRD: 13 security — SSO
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- ADRs: 0007 gateway
- APIs: system.yaml (auth config)

## Testing
- Form validations; mapping preview; test login; masking; RBAC; a11y/perf

## Change Log
| Date       | Version | Description                         | Author |
|------------|---------|-------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — SSO admin UI (12)   | PO     |

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

