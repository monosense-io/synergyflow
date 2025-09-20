---
id: epic-05-knowledge
title: Epic 05 — Knowledge Management
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 05 — Knowledge Management

## Epic Goal
Implement a governed knowledge base with versioning, review workflows, and high‑performance search to accelerate resolution (PRD 2.7).

## Scope
- In scope: Article lifecycle (Draft→Review→Publish), versioning/rollback, attachments, search relevance, related articles, expiry notifications.
- Out of scope: Analytics beyond usage KPIs (Epic 09/10).

## Key Requirements
- PRD 2.7: Role-based approvals; version history with rollback; full‑text search under 800ms (p95); related article suggestions; expiry reminders.

Refs: docs/prd/02-itil-v4-core-features-specification.md

## Architecture & ADR References
- Data & indexing: docs/architecture/data-architecture.md, docs/architecture/testing-architecture.md
- Cache/store: docs/adrs/0011-cache-store-dragonflydb-over-redis.md
- Event bus & versioning: docs/adrs/0001-event-only-inter-module-communication.md, docs/adrs/0004-event-contract-versioning.md

## API & Events Impact
- APIs: docs/api/modules/knowledge.yaml, docs/api/modules/search.yaml, docs/api/modules/users.yaml
- Events: kb.article.created/updated/published, kb.article.expiring

## Data Model Impact
- Entities: KnowledgeArticle, ArticleVersion, Attachment, Tag, ReviewTask.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 03 (Portal) for end-user search; Epic 11 (Notifications) for expiry; Epic 12 (Security) for approvals.

## Risks & Mitigation
- Search relevance drift → telemetry-driven boosting; synonym/stemming management.
- Content staleness → expiry workflow and ownership policy.

## Acceptance Criteria (Epic)
- Article workflow enforced with approvals; search meets p95 latency target; version rollback functional.

## Story Seeds
1) Article model with versioning and attachments.
2) Approval workflow + role policies.
3) Search indexing and relevance tuning.
4) Related article suggestion service.
5) Expiry notification scheduler.

## Verification & Done
- Search load/perf tests; workflow e2e tests; audit trails validated.

