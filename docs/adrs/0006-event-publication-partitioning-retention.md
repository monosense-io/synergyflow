---
adr: 0006
title: Event Publication Partitioning & Retention
status: Accepted
date: 2025-09-03
owner: DBA/Architect
links:
  - ../prd/26-db-schema-delta-additions-changes.md
---

Decision

- Monthly partitions; active retention ≤ 7 days; archive to object storage; scheduled cleanup.

Rationale

- Sustain 2000+ TPS with predictable storage/maintenance overhead.

Consequences

- Operational jobs required; observability for lag and table growth.
