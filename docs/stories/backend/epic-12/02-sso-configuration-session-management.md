Status: Draft

# Story
As a security administrator,
I want SSO configuration and hardened session management,
so that user authentication is centralized, secure, and compliant.

## Acceptance Criteria
1. Support OIDC (and/or SAML) IdP configuration with discovery, client credentials, allowed domains, and group claims mapping to roles.
2. Session management with refresh tokens/rotations, idle/absolute timeouts, CSRF protections, and device/session listing + revoke.
3. Login/logout flows at gateway integrated with backend sessions; remember‑device policy optional with MFA hooks (if configured).
4. Security events emitted (auth.login.success/failure, auth.logout, session.revoked) with rate‑limited failure logs.
5. Admin APIs for IdP config, callback URLs validation, and test login; audit all changes; secrets stored via SecretRef.

## Tasks / Subtasks
- [ ] IdP config model and CRUD; callback validation (AC: 1, 5)
- [ ] Group claim→role mapping and provisioning (AC: 1)
- [ ] Session manager (rotation, timeouts, CSRF, revoke/list) (AC: 2)
- [ ] Gateway login/logout integration and MFA hook (AC: 3)
- [ ] Security events and audit logging; SecretRef for sensitive fields (AC: 4, 5)

## Dev Notes
- PRD: docs/prd/13-security-compliance-framework.md (SSO and auth requirements)
- Architecture: security-architecture.md; gateway-envoy.md; observability-reliability.md (security events)
- ADRs: 0007 gateway placement; 0010 Envoy Gateway; 0009 DB migrations; 0001 event bus
- APIs: users.yaml (identity), system.yaml (auth config)

## Testing
- IdP config validation and test login; group→role mapping
- Session rotation/timeout/CSRF tests; revoke/list
- Event emission and audit coverage

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — SSO & session management      | PO     |

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

