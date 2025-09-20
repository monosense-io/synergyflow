---
id: epic-16-deployment-ops
title: Epic 16 — Deployment & Operations
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 16 — Deployment & Operations

## Epic Goal
Operationalize deployment, migrations, observability, and runbooks to meet SLOs and support safe releases (PRD 20).

## Scope
- In scope: CI/CD strategies, blue/green/canary, migration orchestration, secrets/config, observability, runbooks.
- Out of scope: Non-product infra provisioning.

## Key Requirements
- PRD 20: Deployment process, operations practices, rollback procedures, monitoring, and incident response.

Refs: docs/prd/20-deployment-operations.md

## Architecture & ADR References
- Deployment & k8s: docs/architecture/deployment-architecture.md, docs/architecture/k8s/ (if applicable)
- Migrations: docs/architecture/migrations.md, docs/adrs/0009-db-migration-schema-evolution.md
- HA posture: docs/adrs/0003-ha-posture-single-dc-rack-ha.md
- Gateway ops: docs/adrs/0010-envoy-gateway-adoption-k8s-gateway-api.md

## API & Events Impact
- APIs: docs/api/modules/system.yaml
- Events: deploy.started/completed, migration.applied, rollback.executed, incident.opened

## Data Model Impact
- Entities: DeploymentPlan, Rollout, MigrationBatch, Runbook, AlertRoute.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 02 (Release) for orchestration; Epic 13 (Reliability) for SLOs; Epic 11 (Notifications) for alerts; Epic 14 (Testing) for gates.

## Risks & Mitigation
- Migration failures → idempotent, reversible migrations; dry-run; preflight checks.
- Observability blind spots → SLO-first instrumented services and runbooks.

## Acceptance Criteria (Epic)
- Safe rollout patterns implemented; migrations orchestrated with evidence; observability dashboards and alerting tuned; runbooks published.

## Story Seeds
1) CI/CD workflows and environment promotion.
2) Migration orchestration with evidence.
3) Rollout strategy toolkit (blue/green/canary).
4) Runbooks and on-call procedures.

## Verification & Done
- Deployment game-days; rollback drills; alert fatigue assessment.

