---
id: epic-07-itam
title: Epic 07 — IT Asset Management (ITAM)
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 07 — IT Asset Management (ITAM)

## Epic Goal
Establish discovery-driven hardware/software asset tracking, ownership, lifecycle, and license compliance (PRD 2.8).

## Scope
- In scope: Discovery/import, inventory, ownership/location/lifecycle, license usage vs entitlements, barcode/QR workflows, CI links.
- Out of scope: Procurement.

## Key Requirements
- PRD 2.8: Populate inventory from discovery/import; track ownership/location/lifecycle with history; monitor license compliance; mobile barcode/QR updates; link assets to tickets.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Data architecture and access: docs/architecture/data-architecture.md, docs/architecture/data-access-mybatis-plus.md
- DB evolution: docs/adrs/0009-db-migration-schema-evolution.md
- Event bus: docs/adrs/0001-event-only-inter-module-communication.md

## API & Events Impact
- APIs: docs/api/modules/cmdb.yaml, docs/api/modules/incidents.yaml
- Events: asset.discovered, asset.updated, license.threshold.breached

## Data Model Impact
- Entities: Asset, AssetType, Ownership, Location, LifecycleEvent, LicenseEntitlement.
- Refs: docs/prd/18-data-model-overview.md, docs/prd/26-db-schema-delta-additions-changes.md

## Dependencies
- Epic 08 (CMDB) for CI relationships; Epic 11 (Notifications); Epic 03/01 for linking to tickets.

## Risks & Mitigation
- Discovery noise → normalization/merge rules; confidence scores.
- License data accuracy → scheduled reconciliations; audit logs.

## Acceptance Criteria (Epic)
- Inventory maintained with history; license compliance alerts; asset-ticket impact visible.

## Story Seeds
1) Import/discovery pipelines and normalization.
2) Asset model and ownership/lifecycle tracking.
3) License monitoring and alerts.
4) Mobile barcode/QR update flows.

## Verification & Done
- Reconciliation tests; audit completeness; license compliance reports.

