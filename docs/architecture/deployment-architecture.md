---
id: arch-03-deployment-architecture
title: Deployment Architecture (On‑Prem Single DC, Rack‑Level HA)
owner: Infra/Platform
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/09-high-availability-reliability-architecture.md
  - ../prd/20-deployment-operations.md
---

## Topology

- Single Kubernetes cluster in one data center
- HA across nodes/racks (no multi‑AZ dependency)
- Envoy Gateway (Gateway API) for L7 ingress; CloudNative‑PG primary + sync
  standby; Redis (Cluster/Sentinel); MinIO (erasure coding)

## DR

- Optional remote DR site (cold/warm); PostgreSQL WAL + MinIO replication

## Performance Targets

- 2000+ TPS event publication registry; 350 concurrent users baseline
