---
id: arch-04-data-architecture
title: Data Architecture & Storage
owner: Architect (Data)
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/18-data-model-overview.md
  - ../prd/26-db-schema-delta-additions-changes.md
  - ../../migrations/
---

## Stores

- PostgreSQL OLTP (partitioned event_publication)
- MinIO object storage (attachments, artifacts)
- DragonflyDB (Redis‑compatible) for cache/session

## Partitioning & Retention

- event_publication partitioned by month; 7‑day active retention with archive
- Logs: 30‑day hot, 6–12 months warm, long‑term archive per policy

## Schema Evolution

- Versioned migrations under migrations/ (V001..)
- Rollback guidance inline in each migration
