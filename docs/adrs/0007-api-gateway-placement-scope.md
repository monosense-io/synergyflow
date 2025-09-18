---
adr: 0007
title: API Gateway Placement & Scope
status: Accepted
date: 2025-09-03
owner: Architect (Integration)
links:
  - ../prd/05-integration-architecture.md
---

Decision
- Spring Cloud Gateway as ingress for REST/GraphQL/Webhooks; internal/external routes; centralized auth, rate limiting, and observability.

Rationale
- Consistent security and policy enforcement; single front door.

Consequences
- Gateway HA must match app SLOs; careful route ownership and versioning.

