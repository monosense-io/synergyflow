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

Operations

- Manage Gateway/GatewayClass/Routes via GitOps.
- CI validates manifests (schema, policy, lint) before deploy.
- Dashboards for 4xx/5xx, latency, rate-limit hits; alert on SLO breach.
