Status: Draft

# Story
As a platform engineer,
I want API gateway deployment with baseline policies configured,
so that services are exposed securely with consistent routing, auth, and rate limits.

## Acceptance Criteria
1. Deploy Envoy Gateway (per ADR‑0010) with base listeners/routes; upstream services registered via manifests.
2. Policies applied: authentication (JWT/OIDC), authorization (RBAC integration), TLS termination, request/response size limits, and timeouts.
3. RateLimitPolicy supports per‑client and per‑route limits; bursting and quotas configurable; enforcement verified.
4. WAF rules and anomaly detection integrated (at least basic ruleset enabled) with allow/deny lists and logging.
5. Health checks and canary routing supported; blue/green toggles documented for Epic 16 use; all policies as code in repo with CI validation.

## Tasks / Subtasks
- [ ] Gateway manifests and deployment (AC: 1)
- [ ] AuthN/Z policy config (JWT/OIDC + RBAC hook) (AC: 2)
- [ ] Rate limiting service and policies (AC: 3)
- [ ] WAF ruleset and logging integration (AC: 4)
- [ ] Health checks and canary/blue‑green routing docs (AC: 5)

## Dev Notes
- PRD: docs/prd/05-integration-architecture.md
- Architecture: docs/architecture/gateway-envoy.md; docs/architecture/api-architecture.md
- ADRs: 0010 Envoy Gateway; 0007 gateway placement; 0004 event contract versioning (downstream contracts unaffected but relevant for events)
- Data models involved: RateLimitPolicy; ApiClient behavior for enforcement
- APIs: openapi.yaml for routes; system.yaml for policy endpoints

## Testing
- Gateway health; auth policies; rate limit enforcement; WAF rule triggers; canary route verification

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — gateway deployment & policies   | PO     |

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

