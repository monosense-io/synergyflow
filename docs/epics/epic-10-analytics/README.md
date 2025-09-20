---
id: epic-10-analytics
title: Epic 10 — Advanced Analytics Integration
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 10 — Advanced Analytics Integration

## Epic Goal
Integrate with analytics platforms/warehouses for curated datasets, BI connectivity, and optional ML pipelines (PRD 2.12).

## Scope
- In scope: Curated datasets for incidents/changes/assets/SLAs; BI connectivity/permissions; refresh scheduling; lineage; optional ML with monitoring.
- Out of scope: Real-time dashboards (Epic 09).

## Key Requirements
- PRD 2.12: Publish datasets; validate BI tools access; schedule refresh; document lineage/governance; ML pipelines with drift monitoring.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Event streams & partitioning: docs/adrs/0006-event-publication-partitioning-retention.md
- Data architecture: docs/architecture/data-architecture.md

## API & Events Impact
- APIs: docs/api/openapi.yaml (export endpoints where applicable)
- Events: dataset.refresh.requested/completed, ml.model.updated

## Data Model Impact
- Entities: Dataset, DataContract, RefreshSchedule, LineageRecord, Model, DriftMetric.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 09 (Reports) for measures; Epic 13 (Reliability) for pipeline SLOs; Epic 12 (Security) for access control.

## Risks & Mitigation
- Data contract breaks → versioned contracts and backward compatibility windows.
- PII governance → masking/pseudonymization and access auditing.

## Acceptance Criteria (Epic)
- Curated datasets available; BI connectivity validated; refresh schedules; lineage documented; ML pipelines monitored.

## Story Seeds
1) Dataset contracts and export services.
2) BI connectivity/permissions and row-level security.
3) Refresh scheduler and lineage tracker.
4) ML pipeline template with monitoring.

## Verification & Done
- Data contract tests; lineage completeness; BI smoke tests.

