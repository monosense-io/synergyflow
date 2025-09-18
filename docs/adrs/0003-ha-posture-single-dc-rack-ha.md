---
adr: 0003
title: HA Posture – Single Data Center, Rack-Level HA
status: Accepted
date: 2025-09-03
owner: Infra/Platform
links:
  - ../prd/09-high-availability-reliability-architecture.md
---

Decision
- Single DC deployment with HA across nodes/racks; CloudNative‑PG primary + sync standby.

Rationale
- Practical on-prem constraints; reliable failover without multi‑AZ.

Consequences
- DR requires optional remote site; egress/firewall rules for external services.

