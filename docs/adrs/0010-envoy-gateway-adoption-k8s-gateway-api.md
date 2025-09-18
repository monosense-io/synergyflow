---
adr: 0010
title: Adopt Envoy Gateway (Kubernetes Gateway API) for Edge Ingress
status: Accepted
date: 2025-09-18
owner: Architect (Integration)
links:
  - ../architecture/api-architecture.md
  - ../architecture/deployment-architecture.md
  - ../architecture/security-architecture.md
  - ../prd/05-integration-architecture.md
  - 0007-api-gateway-placement-scope.md
---

Context

- Original plan referenced Spring Cloud Gateway as the API gateway/front door.
- Production will run on on‑prem Kubernetes with Envoy Gateway already standard.
- We require native support for the Kubernetes Gateway API (Gateway/HTTPRoute),
  Envoy filters (rate limiting, external auth), and consistent platform ops.

Decision

- Adopt Envoy Gateway using the Kubernetes Gateway API as the API front door.
- Use GatewayClass/Gateway for internal/external listeners; HTTPRoute for REST.
- Integrate JWT/OIDC at the gateway edge; use Envoy Rate Limit for quotas.
- Enable TLS termination at the gateway; evaluate mTLS to services where suitable.

Rationale

- Aligns with platform standard (Envoy Gateway) and Gateway API future.
- Rich, battle‑tested Envoy filters (security, rate limit, external auth, WAF).
- Declarative CRDs fit GitOps; portability across Kubernetes vendors.

Consequences

- ADR‑0007 is superseded by this decision.
- Team shifts from Spring Cloud Gateway config to Gateway API manifests.
- Add cluster prerequisites (GatewayClass `envoy`, certificates management).
- CI/CD must validate Gateway/HTTPRoute manifests (schema, policy checks).
Follow‑ups

- Update API Architecture and Deployment docs with Gateway/HTTPRoute examples.
- Add security controls at gateway (JWT validation, rate limit, WAF guidance).
- Create GitOps repo path and policies for gateway objects.
