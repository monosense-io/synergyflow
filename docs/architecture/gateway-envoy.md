---
title: Gateway (Envoy) Configuration
owner: Infra/Platform
status: Draft
last_updated: 2025-09-18
links:
  - api-architecture.md
  - deployment-architecture.md
  - security-architecture.md
---

Overview

- Envoy Gateway is the API front door using the Kubernetes Gateway API.
- Separate Gateways/listeners for external and internal audiences.
- Use HTTPRoute for REST; GRPCRoute for gRPC if introduced later.

Gateway & Routes (Examples)

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: GatewayClass
metadata:
  name: envoy
spec:
  controllerName: gateway.envoyproxy.io/gatewayclass-controller
```

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: synergyflow-gw
  namespace: edge
spec:
  gatewayClassName: envoy
  listeners:
    - name: https
      protocol: HTTPS
      port: 443
      hostname: api.synergyflow.example.com
      tls:
        mode: Terminate
        certificateRefs:
          - name: synergyflow-cert
```

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: api-v1
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
            value: /api/v1
      backendRefs:
        - name: synergyflow-backend
          port: 8080
```

Security Controls

- JWT validation at Gateway with OIDC issuer and audience checks.
- External auth for fine-grained policy; integrate with corporate IdP.
- Rate limiting via Envoy Rate Limit service (per API key/user/client).
- mTLS between gateway and backend where feasible.

JWT Validation & Callback Restrictions (Story 16.09)

```yaml
# Envoy Gateway JWT provider (extension)
apiVersion: gateway.envoyproxy.io/v1alpha1
kind: JWT
metadata:
  name: oidc-default
  namespace: edge
spec:
  providers:
    - name: oidc
      issuer:
        url: ${OIDC_ISSUER}
      remoteJWKS:
        uri: ${OIDC_JWKS_URI:-"${OIDC_ISSUER}/protocol/openid-connect/certs"}
        cacheDuration: ${JWKS_CACHE_TTL_SECONDS:-300}s
      audiences:
        - ${SECURITY_JWT_EXPECTED_AUDIENCE}
  claimToHeaders:
    - claim: sub
      header: x-auth-sub
    - claim: roles
      header: x-auth-roles
```

```yaml
# Protected routes; NextAuth callbacks limited and rate limited per token
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
            value: /actuator/health
      backendRefs:
        - name: synergyflow-backend
          port: 8080
    - matches:
        - path:
            type: PathPrefix
            value: /api/auth/
      filters:
        - type: ExtensionRef
          extensionRef:
            group: gateway.envoyproxy.io
            kind: JWT
            name: oidc-default
        - type: ExtensionRef
          extensionRef:
            group: gateway.envoyproxy.io
            kind: RateLimit
            name: auth-per-token
      backendRefs:
        - name: synergyflow-frontend
          port: 80
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

Notes

- Allowed callback hosts are derived from `NEXTAUTH_URL` and optional `NEXTAUTH_CALLBACK_HOSTS`.
- On JWT validation failure, the gateway strips `Authorization` and returns `401`; forbidden paths return `403`.
- JWKS outage behavior is documented per Story 16.09 (clear `5xx` with logs).

Operations

- Manage Gateway/GatewayClass/Routes via GitOps.
- CI validates manifests (schema, policy, lint) before deploy.
- Dashboards for 4xx/5xx, latency, rate-limit hits; alert on SLO breach.
