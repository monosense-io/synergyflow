---
id: epic-12-security-compliance
title: Epic 12 — Security & Compliance
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 12 — Security & Compliance

## Epic Goal
Harden security across authentication, authorization, data protection, and compliance controls (PRD 13).

## Scope
- In scope: RBAC/ABAC, SSO, audit logging, data encryption, secrets management, compliance evidence, vulnerability and configuration management hooks.
- Out of scope: Non-product enterprise tooling.

## Key Requirements
- PRD 13: role-based approvals; auditability; encryption and access controls; compliance frameworks; privacy/PII handling.

Refs: docs/prd/13-security-compliance-framework.md

## Architecture & ADR References
- Security architecture: docs/architecture/security-architecture.md
- API gateway & placement: docs/architecture/gateway-envoy.md, docs/adrs/0007-api-gateway-placement-scope.md, docs/adrs/0010-envoy-gateway-adoption-k8s-gateway-api.md
- DB evolution & evidence: docs/adrs/0009-db-migration-schema-evolution.md

## API & Events Impact
- APIs: docs/api/modules/users.yaml, docs/api/modules/system.yaml
- Events: security.audit.logged, user.role.changed

## Data Model Impact
- Entities: Role, Permission, Policy, AuditEvent, SecretRef.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- All epics consume policies and audit; Epic 11 (Notifications) for security alerts; Epic 14 (Testing & Quality) for compliance checks.

## Risks & Mitigation
- Over-permissioning → least-privilege defaults, periodic attestation.
- Audit gaps → centralized, immutable logs; coverage tests.

## Acceptance Criteria (Epic)
- RBAC/ABAC enforced; SSO configured; audit logs immutable and queryable; encryption in transit/at rest; compliance checks documented.

## Story Seeds
1) Central authz service (RBAC/ABAC) and policy definitions.
2) SSO configuration and session management.
3) Centralized audit pipeline; signed logs.
4) Secrets management integration.

## Verification & Done
- Pen test remediations; policy coverage tests; compliance evidence generated.

