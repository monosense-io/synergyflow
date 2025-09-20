---
id: epic-03-self-service-portal
title: Epic 03 — Self-Service Portal
version: 1.0
last_updated: 2025-09-18
owner: Product Owner
status: Proposed
---

# Epic 03 — Self-Service Portal

## Epic Goal
Deliver a customizable, WCAG-compliant portal for end users to submit and track tickets, search knowledge, and browse the service catalog (PRD 2.5).

## Scope
- In scope: Guided ticket submission, status tracking, integrated knowledge search, catalog browsing, branding/localization, SSO, user preferences.
- Out of scope: Back-office fulfillment (Epic 04), agent UI specifics.

## Key Requirements
- PRD 2.5 acceptance criteria: ticket creation/tracking, KB search with previews and usefulness ratings, catalog browsing with SLAs, branding/localization, SSO, common request templates.

Refs: docs/prd/02-itil-v4-core-features-specification.md, docs/ux/README.md

## Architecture & ADR References
- Security & SSO: docs/architecture/security-architecture.md
- API & gateway: docs/architecture/api-architecture.md, docs/adrs/0007-api-gateway-placement-scope.md
- Mobile push policy: docs/adrs/0008-mobile-push-egress-policy.md

## API & Events Impact
- APIs: docs/api/modules/service-requests.yaml, docs/api/modules/knowledge.yaml, docs/api/modules/search.yaml, docs/api/modules/users.yaml
- Events: request.created, request.updated, kb.viewed, kb.rated

## Data Model Impact
- Entities: Ticket, RequestTemplate, KnowledgeArticle, UserPreference, BrandingTheme, Localization.
- Refs: docs/prd/18-data-model-overview.md

## Dependencies
- Epic 04 (Service Catalog), Epic 05 (Knowledge), Epic 11 (Notifications), Epic 12 (Security & Compliance).

## Risks & Mitigation
- Accessibility gaps → automated WCAG checks in CI; UX review gates.
- Search performance → index tuning and caching; fallback pagination.

## Acceptance Criteria (Epic)
- Users submit/track tickets, browse catalog and search knowledge with performance p95 < 800ms.
- Branding and localization configurable; SSO enabled; preferences persisted.

## Story Seeds
1) Portal skeleton with SSO and preferences.
2) Guided ticket submission with dynamic forms.
3) Knowledge search integration with previews and ratings.
4) Catalog browse and request flows with SLAs.

## Verification & Done
- Accessibility (WCAG 2.1 AA) report; performance checks; UX acceptance.

