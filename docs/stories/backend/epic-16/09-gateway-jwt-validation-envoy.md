Status: Ready for Dev

# Story
As an infrastructure/platform engineer,
I want Envoy Gateway to validate JWTs at the edge and enforce callback restrictions,
so that only authenticated, policy-compliant requests reach backend services.

## Acceptance Criteria
1. Gateway JWT provider configured with IdP settings and committed manifests:
   - Manifests live under `infra/gateway/`:
     - `infra/gateway/jwt-provider.yaml`
     - `infra/gateway/http-route-protected.yaml`
   - Issuer (well-known): `OIDC_ISSUER` (matches backend 16.08)
   - Audiences: `SECURITY_JWT_EXPECTED_AUDIENCE` (matches backend 16.08)
   - JWKS cache TTL and retry policy set (default TTL 300s; backoff enabled)
2. HTTPRoute updates for protected APIs:
   - Attach JWT filter requiring valid tokens on `/api/**`.
   - Allow unauthenticated access to `/api/public/**` and `/actuator/health`.
3. Header sanitation and propagation:
   - Strip incoming `Authorization` on validation failure (no backend pass-through).
   - After successful validation, pass selected headers only: `x-auth-sub`, `x-auth-roles` (derived from claims), nothing else.
   - Error mapping documented: invalid/expired/wrong-aud → `401`; forbidden paths → `403`.
4. Callback/redirect restrictions:
   - Only allow NextAuth callback paths (`/api/auth/*`) to reach the frontend Service.
   - Allowed callback hosts sourced from env: `NEXTAUTH_URL` (single) and optional `NEXTAUTH_CALLBACK_HOSTS` (comma-separated list) for staging.
   - Block external callback attempts that do not match configured hosts.
5. Rate limiting (baseline):
   - Per-token limit on `/api/auth/*`: default `60` requests/minute.
   - Configurable via env `GATEWAY_RATE_LIMIT_PER_TOKEN`.
6. TLS/mTLS:
   - TLS termination at Gateway with valid certs.
   - Document optional mTLS between Gateway and backend for high-sensitivity paths.
7. Documentation:
   - Provide example GatewayClass, Gateway, and HTTPRoute manifests with JWT filter configuration.
   - Add operational runbook: rotate keys, IdP outage behavior (JWKS fetch failures), and error codes surfaced to clients.
8. Validation:
   - e2e: valid JWT passes to backend `/api/whoami`; invalid/expired/wrong audience → `401` from Gateway.
   - IdP/JWKS outage: Gateway returns a clear `5xx` (implementation-specific; document chosen status) and logs cause.
   - `/actuator/health` remains reachable unauthenticated.

## Prerequisites
- Story 16.08 (Backend OAuth2 Resource Server JWT) is complete and deployed with the same `OIDC_ISSUER` and `SECURITY_JWT_EXPECTED_AUDIENCE`.

## Tasks / Subtasks
- [ ] Create gateway manifests (commit to `infra/gateway/`): (AC: 1, 2)
  - [ ] `infra/gateway/jwt-provider.yaml`
  - [ ] `infra/gateway/http-route-protected.yaml`
- [ ] Configure header pass-through (`x-auth-sub`, `x-auth-roles`) and `Authorization` stripping on failure (AC: 3)
- [ ] Restrict callbacks to `NEXTAUTH_URL`/`NEXTAUTH_CALLBACK_HOSTS`; block others (AC: 4)
- [ ] Add per-token rate-limit for `/api/auth/*` (env `GATEWAY_RATE_LIMIT_PER_TOKEN`, default 60/min) (AC: 5)
- [ ] Document TLS and optional mTLS topology (AC: 6)
- [ ] Update `docs/architecture/gateway-envoy.md` with the final JWT filter, callbacks, and rate-limit config (AC: 7)
- [ ] Write e2e validation steps and expected responses, including IdP/JWKS outage behavior (AC: 8)

## Dev Notes
- Prefer Gateway API resources (Gateway/HTTPRoute) with Envoy extensions.
- JWT validation can be configured using Envoy `jwt_authn` with remote JWKs and audiences; ensure clock skew handling.
- For claims-to-headers mapping, avoid forwarding sensitive PII. Use minimal subject/roles.
- Consider separate HTTPRoute for public vs protected prefixes to keep policy explicit.

## Example (Illustrative)
```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: api-protected
  namespace: edge
spec:
  parentRefs:
    - name: synergyflow-gw
  hostnames:
    - api.synergyflow.example.com
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /api
      filters:
        - type: ExtensionRef
          extensionRef:
            group: gateway.envoyproxy.io
            kind: JWT
            name: oidc-default
      backendRefs:
        - name: synergyflow-backend
          port: 8080
```

## Testing
- With a valid token (issuer matches, audience contains expected value), `/api/whoami` returns 200 via Gateway.
- With an expired token or wrong audience, Gateway returns 401; logs contain validation reason.
- During IdP/JWKS outage, Gateway returns a clear 5xx per configuration; behavior documented.
- `/actuator/health` is accessible without a token.

## Change Log
| Date       | Version | Description                              | Author |
|------------|---------|------------------------------------------|--------|
| 2025-09-20 | 0.1     | Initial draft — Envoy JWT validation     | PM     |
| 2025-09-21 | 0.2     | PO course-correction — clarified manifests, callbacks, rate-limit, and outage handling | PO     |

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List
- docs/architecture/gateway-envoy.md
- infra/gateway/oidc-jwt.yaml
- infra/gateway/routes-api.yaml

## QA Results
### Review Date: TBD

### Reviewed By: Quinn (Test Architect)

### Gate Decision
- Status: TBD

### QA References
- Risk profile: `docs/qa/assessments/16.09-risk-YYYYMMDD.md`
- Test design: `docs/qa/assessments/16.09-test-design-YYYYMMDD.md`
