---
id: epic-11-notifications
title: Epic 11 — Notifications & Communication
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 11 — Notifications & Communication

## Epic Goal
Provide reliable notifications (email, push, in-app) for status changes, SLA warnings, approvals and escalations (PRD 7).

## Scope
- In scope: Notification templates, channels (email/push/in-app), preferences, routing by severity/role, deduplication and rate-limits, audit and retries.
- Out of scope: External marketing communications.

## Key Requirements
- PRD 7: Notifications on key status changes (submitted, approved, scheduled, implemented), SLA pre-breach, configurable preferences, localization.

Refs: docs/prd/07-notification-system-design.md

## Architecture & ADR References
- Event bus and partitioning: docs/adrs/0001-event-only-inter-module-communication.md, docs/adrs/0006-event-publication-partitioning-retention.md
- Mobile push egress: docs/adrs/0008-mobile-push-egress-policy.md
- Gateway policies: docs/architecture/gateway-envoy.md

## API & Events Impact
- APIs: docs/api/modules/system.yaml, docs/api/modules/users.yaml, docs/api/modules/events.yaml
- Events: notification.dispatched, notification.failed, preference.updated

## Data Model Impact
- Entities: NotificationTemplate, Preference, Subscription, Delivery, Channel.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- All domain epics as producers; Epic 12 (Security) for permissions and PII handling; Epic 09 (Reports) for delivery KPIs.

## Risks & Mitigation
- Alert fatigue → dedupe, grouping, digesting, and per-channel thresholds.
- Delivery reliability → retries with backoff; dead-letter queues; provider failover.

## Acceptance Criteria (Epic)
- Channel delivery with preferences and localization; SLA alerts; audit trails and failure handling.

## Story Seeds
1) Template and localization system.
2) Preference/Subscription model and APIs.
3) Channel adapters (email/push/in-app) with retries.
4) Dedupe/grouping/digests.

## Verification & Done
- End-to-end delivery tests; failure simulations; latency KPIs.

