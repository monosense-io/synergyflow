---
id: arch-06-security-architecture
title: Security Architecture
owner: Security
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/13-security-compliance-framework.md
---

## Controls

- RBAC and module boundary enforcement (Modulith verify)
- Event payload sanitization (Sensitive fields)
- API auth (OIDC/SAML) at edge via Envoy external auth / JWT filter
- Rate limiting (client/user) via Envoy Rate Limit service
- Data retention, GDPR/SOX/ISO 27001 alignment

## Monitoring

- SIEM/SOAR integration; boundary violation alerts; auth anomaly detection

## Threat Model (Draft)

- Actors: external users, internal agents, integrators, admin roles.
- Assets: PII in tickets, credentials/tokens, event data, CI relationships.
- Risks: privilege escalation, event poisoning, data exfiltration, CSRF.
- Mitigations: RBAC, input validation, output encoding, rate limiting, CSP, secrets via Vault.
