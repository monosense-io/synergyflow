# Envoy Gateway Manifests (Story 16.09)

These manifests define the Envoy Gateway configuration for JWT validation at the edge, callback restrictions, and rate limiting.

## Contents
- `gatewayclass.yaml` – Envoy GatewayClass
- `gateway.yaml` – External Gateway (HTTPS listener)
- `jwt-provider.yaml` – Envoy Gateway JWT provider (OIDC issuer, JWKS, audiences, claim-to-headers)
- `http-route-protected.yaml` – Protected HTTPRoute rules (public health, `/api/auth/*` with JWT + rate limit, all `/api/**` with JWT)
- `rate-limit-auth.yaml` – Per-token rate limit policy for `/api/auth/*`

## Prerequisites
- Cluster has the Gateway API and Envoy Gateway CRDs installed.
- Namespace `edge` exists (or update `metadata.namespace` accordingly).
- Backend Services exist and match names/ports in routes:
  - `synergyflow-backend:8080`
  - `synergyflow-frontend:80`
- TLS certificate secret `synergyflow-cert` exists in namespace `edge` (or update the reference in `gateway.yaml`).

## Environment Parameters
The manifests expect the following values (used in `jwt-provider.yaml` and referenced by story 16.09):

- `OIDC_ISSUER` – OIDC issuer URL (must match backend Story 16.08)
- `OIDC_JWKS_URI` – Optional; JWKS URI (defaults to `${OIDC_ISSUER}/protocol/openid-connect/certs`)
- `JWKS_CACHE_TTL_SECONDS` – JWKS cache TTL (default `300`)
- `SECURITY_JWT_EXPECTED_AUDIENCE` – Audience expected by both gateway and backend
- `GATEWAY_RATE_LIMIT_PER_TOKEN` – Requests per minute for `/api/auth/*` (default `60`)
- `NEXTAUTH_URL` / `NEXTAUTH_CALLBACK_HOSTS` – Callback hosts enforced by gateway policy (documented in story/arch)

Note: Kubernetes does not perform shell-style `${VAR}` substitution. Use one of the approaches below to render values before `kubectl apply`.

## Applying Manifests

### Option A: envsubst (simple)
```bash
export OIDC_ISSUER=https://keycloak.example.com/realms/synergyflow
export SECURITY_JWT_EXPECTED_AUDIENCE=synergyflow-api
export JWKS_CACHE_TTL_SECONDS=300
export GATEWAY_RATE_LIMIT_PER_TOKEN=60

for f in infra/gateway/*.yaml; do
  envsubst < "$f" | kubectl apply -n edge -f -
done
```

### Option B: Kustomize/Helm (recommended for multi-env)
- Create overlays per environment and template values per cluster.
- Ensure CI validates rendered manifests.

## CI Validation
A GitHub Actions workflow validates YAML and performs schema checks:
- `.github/workflows/infra-validate.yml` – Runs `yamllint` and `kubeconform` on changes under `infra/gateway/`.
- `kubeconform` runs with `-ignore-missing-schemas` to tolerate CRDs not present in the default catalog (e.g., Envoy Gateway extensions).

## Operational Notes
- On JWT validation failure, the gateway strips `Authorization` and returns `401`; forbidden paths return `403`.
- Documented JWKS outage behavior: surface a clear `5xx` and log cause.
- Allowed callback hosts derived from `NEXTAUTH_URL` and optional `NEXTAUTH_CALLBACK_HOSTS`.

## Next Steps
- Wire GitOps and environment overlays.
- Add policy checks (OPA/Conftest) if required by your organization.
- Extend dashboards to track 401/403 rate and JWKS errors for alerting.

