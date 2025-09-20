Status: Draft

# Story
As a platform owner,
I want a security settings and compliance UI,
so that I can view encryption/secrets posture, control statuses, and exceptions in one place.

## Acceptance Criteria
1. Overview shows encryption status (in transit/at rest), secret references coverage, and policy versions with last attestation.
2. Controls tab lists key controls (authz coverage, audit retention, SSO config) with pass/fail and remediation links.
3. Exceptions tab shows accepted risks with expiry; actions to request/approve/revoke exceptions with rationale.
4. Export compliance posture report; permissions enforced; audit all admin actions.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Overview widgets and data fetch (AC: 1)
- [ ] Controls list with remediation links (AC: 2)
- [ ] Exceptions management UI (AC: 3)
- [ ] Export and permissions/audit hooks (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-12/05-compliance-evidence-controls.md
- PRD: 13 security & compliance
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (compliance)

## Testing
- Data accuracy; exceptions workflows; export; a11y/perf

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — security settings & compliance | PO     |

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

