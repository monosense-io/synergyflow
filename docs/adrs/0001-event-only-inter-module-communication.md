---
adr: 0001
title: Event-Only Inter-Module Communication
status: Accepted
date: 2025-09-03
owner: Architect
links:
  - ../prd/03-system-architecture-monolithic-approach.md
---

Decision

- All ITIL modules communicate via domain events; no direct module dependencies.

Rationale

- Reduce coupling, improve testability, leverage Modulith boundaries.

Consequences

- Event versioning required; eventual consistency; stronger observability.
