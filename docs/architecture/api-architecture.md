---
id: arch-09-api-architecture
title: API Architecture
owner: Architect (Integration)
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/05-integration-architecture.md
---

## Gateway & Endpoints
- Spring Cloud Gateway as front door (internal/external scopes)
- REST (OpenAPI), GraphQL (complex reads), Webhooks

## Policies
- Rate limiting: 600 req/min per client; 120 req/min per user
- Auth: API Keys/OAuth2; SSO via OIDC/SAML for UI

## Egress
- FCM/APNs; SMTP; external analytics/BI; webhooks

## Governance
- OpenAPI: generated and versioned per module; aggregated at gateway.
- Style: consistent error envelope `{ code, message, details, correlationId }`.
- Reviews: API changes require schema diff review and backward-compatibility check.
- Docs: publish OpenAPI via developer portal; keep examples in sync with event contracts.
