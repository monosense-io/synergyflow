---
adr: 0011
title: Cache Store Selection — DragonflyDB over Redis
status: Accepted
date: 2025-09-18
owner: Architect (Data)
links:
  - ../architecture/data-architecture.md
  - ../prd/10-technology-stack-specifications.md
  - ../architecture/deployment-architecture.md
---

Context

- Initial documentation referenced Redis 7.x (Cluster/Sentinel) for cache/session.
- Operational constraints prefer a simpler, high‑throughput, Redis‑protocol compatible store
  that performs well on on‑prem Kubernetes without complex clustering.

Decision

- Adopt DragonflyDB as the cache/session store. It is Redis‑compatible and
  works with Spring Data Redis and Spring Session with minimal changes.

Rationale

- High throughput with low operational overhead on single‑node or simple HA setups.
- Drop‑in protocol compatibility with existing Redis clients and libraries.
- Suitable for on‑prem Kubernetes; persistence + pod anti‑affinity provide HA posture.

Consequences

- Replace references to Redis with DragonflyDB in architecture and PRD docs.
- Ensure Helm/K8s manifests provision DragonflyDB with persistence and proper anti‑affinity.
- Testcontainers: use Redis container as a protocol‑compatible stand‑in for CI, or Dragonfly image where available.

Migration Notes

- Connection URL remains Redis‑style; validate client timeouts and reconnect logic.
- Session data and caches do not require data migration if established fresh.
