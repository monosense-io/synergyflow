---
id: epic-08-cmdb-impact
title: Epic 08 — CMDB & Impact Analysis
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 08 — CMDB & Impact Analysis

## Epic Goal
Build a CI repository with relationships, snapshots, and impact analysis for incidents/changes (PRD 2.9).

## Scope
- In scope: CI CRUD with type-specific attrs, relationship visualization, impact traversal, baseline/snapshot with diff, pre-approval impact checks.
- Out of scope: Asset procurement (Epic 07 covers assets lifecycle).

## Key Requirements
- PRD 2.9: CI lifecycle and owners; graph traversal; impact analysis during changes/incidents; baselines/snapshots with diff view; event-driven impact to Change module.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- CMDB impact handshake: docs/adrs/0005-cmdb-impact-assessment-handshake.md
- Event bus & versioning: docs/adrs/0001-event-only-inter-module-communication.md, docs/adrs/0004-event-contract-versioning.md
- Data architecture: docs/architecture/data-architecture.md

## API & Events Impact
- APIs: docs/api/modules/cmdb.yaml, docs/api/modules/changes.yaml
- Events: ci.created/updated, ci.relationship.updated, change.impact.assessed

## Data Model Impact
- Entities: CI, CIType, Relationship, Snapshot, Baseline, ImpactAssessment.
- Refs: docs/prd/18-data-model-overview.md, docs/prd/26-db-schema-delta-additions-changes.md

## Dependencies
- Epic 02 (Change) for risk checks; Epic 07 (ITAM) for asset-CI mapping; Epic 01 (Incidents) for impact views.

## Risks & Mitigation
- Relationship accuracy → validation rules; ownership governance; data quality dashboards.
- Graph query performance → indexing/graph store consideration; caching.

## Acceptance Criteria (Epic)
- Impact analysis available pre-approval and during incidents; relationship graph usable at scale; diffs across snapshots.

## Story Seeds
1) CI model with type-specific attributes and owners.
2) Relationship graph and traversal APIs.
3) Baseline/snapshot management with diffs.
4) Impact service integrated with Change/Incident flows.

## Verification & Done
- Impact assessment tests; performance checks; governance policies documented.

