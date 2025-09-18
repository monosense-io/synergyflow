---
id: arch-09-api-architecture
title: API Architecture
owner: Architect (Integration)
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/05-integration-architecture.md
---

## Gateway & Endpoints

- Envoy Gateway (Kubernetes Gateway API) as the front door
  - GatewayClass/Gateway for internal and external listeners
  - HTTPRoute/GRPCRoute/TCPRoute for routing policies
- REST (OpenAPI), GraphQL (complex reads), Webhooks

## Policies

- Rate limiting: 600 req/min per client; 120 req/min per user (Envoy Rate Limit)
- Auth: API Keys/OAuth2; SSO via OIDC/SAML for UI
- JWT validation and external auth at the gateway edge (OIDC provider)
- mTLS between gateway and services where feasible

## Egress

- FCM/APNs; SMTP; external analytics/BI; webhooks

## Gateway (Envoy) Configuration (Sketch)

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
      tls:
        mode: Terminate
        certificateRefs:
          - name: synergyflow-cert
      hostname: api.synergyflow.example.com
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

## Governance

- OpenAPI: generated and versioned per module; aggregated at gateway.
- Style: consistent error envelope `{ code, message, details, correlationId }`.
- Reviews: API changes require schema diff review and backward-compatibility check.
- Docs: publish OpenAPI via developer portal; keep examples in sync with event contracts.
