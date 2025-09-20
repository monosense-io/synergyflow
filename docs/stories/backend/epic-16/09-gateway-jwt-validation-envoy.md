Status: Draft

# Story
As an infrastructure/platform engineer,
I want Envoy Gateway to validate JWTs at the edge and enforce callback restrictions,
so that only authenticated, policy-compliant requests reach backend services.

## Acceptance Criteria
1. Gateway JWT provider configured with IdP settings:
   - Issuer (well-known): `OIDC_ISSUER`
   - Audiences: `SECURITY_JWT_EXPECTED_AUDIENCE`
   - JWKs cache TTL and retry policy set.
2. HTTPRoute updates for protected APIs:
   - Attach JWT filter requiring valid tokens on `/api/**`.
   - Allow unauthenticated access to `/api/public/**` and `/actuator/health`.
3. Header sanitation and propagation:
   - Strip incoming `Authorization` on failure.
   - Optionally pass selected claims as headers (`x-auth-sub`, `x-auth-roles`) only after validation.
4. Callback/redirect restrictions:
   - Only allow NextAuth callback paths (`/api/auth/*`) to reach the frontend Service.
   - Block external callback attempts that do not match configured hosts.
5. Rate limiting (baseline):
   - Configure per-IP or per-token basic limit for `/api/auth/*` endpoints to reduce abuse.
6. TLS/mTLS:
   - TLS termination at Gateway with valid certs.
   - Document mTLS option between Gateway and backend for high-sensitivity paths.
7. Documentation:
   - Provide example GatewayClass, Gateway, and HTTPRoute manifests with JWT filter configuration.
   - Add operational runbook: rotate keys, IdP outage behavior, error codes surfaced to clients.
8. Validation:
   - e2e: valid JWT passes to backend; invalid/expired → 401; insufficient audience → 401.
   - Ensure `/actuator/health` remains reachable unauthenticated.

## Tasks / Subtasks
- [ ] Create/revise Gateway manifests with JWT provider and filters (AC: 1, 2)
- [ ] Configure header pass-through and sanitation (AC: 3)
- [ ] Restrict callback/redirect paths for NextAuth (AC: 4)
- [ ] Add rate-limit policy for `/api/auth/*` (AC: 5)
- [ ] Document TLS and optional mTLS topology (AC: 6)
- [ ] Add example manifests to `docs/architecture/gateway-envoy.md` and `infra/` directory (AC: 7)
- [ ] Write e2e validation steps and expected responses (AC: 8)

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
- With a valid token (issuer matches, audience contains expected value), `/api/whoami` returns 200.
- With an expired token or wrong audience, Gateway returns 401.
- `/actuator/health` is accessible without a token.

## Change Log
| Date       | Version | Description                              | Author |
|------------|---------|------------------------------------------|--------|
| 2025-09-20 | 0.1     | Initial draft — Envoy JWT validation     | PM     |

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

