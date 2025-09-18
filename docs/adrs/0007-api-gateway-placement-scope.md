---
adr: 0007
title: API Gateway Placement & Scope (Superseded)
status: Superseded
date: 2025-09-03
owner: Architect (Integration)
links:
  - ../prd/05-integration-architecture.md
---

Decision

- Spring Cloud Gateway was selected as ingress for REST/GraphQL/Webhooks;
  internal/external routes; centralized auth, rate limiting, and observability.
  Superseded by ADR‑0010 adopting Envoy Gateway (Kubernetes Gateway API).

Rationale

- Consistent security and policy enforcement; single front door.

Consequences

- Gateway HA must match app SLOs; careful route ownership and versioning.
