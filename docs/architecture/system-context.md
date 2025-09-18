---
id: arch-01-system-context
title: System Context
owner: Architect
status: Draft
last_updated: 2025-09-03
links:
  - ../prd/05-integration-architecture.md
  - ../prd/03-system-architecture-monolithic-approach.md
  - ../prd/09-high-availability-reliability-architecture.md
---

## Purpose

Define external actors and systems interacting with SynergyFlow and clarify the product boundary.

## Context Diagram (Logical)

```text
Users (End Users, Agents, Managers)
   │ Web / Mobile
   ▼
SynergyFlow Monolith (Spring Modulith)
   │ REST / GraphQL / Webhooks
   ▼
Integration Adapters (LDAP/AD, SMTP/IMAP, Monitoring, SIEM/SOAR, ERP/CRM/HR)
```

## Context Diagram (Mermaid)

```mermaid
flowchart TB
  Users[Users\n(End Users, Agents, Managers)] -->|Web / Mobile| UI[UI / API Gateway]
  UI --> Monolith[SynergyFlow Monolith\n(Spring Modulith)]
  Monolith -->|REST / GraphQL / Webhooks| Integrations[Integration Adapters]
  subgraph External Systems
    LDAP[LDAP/AD]
    SSO[OIDC/SAML]
    SMTP[SMTP/IMAP]
    MON[Monitoring]
    SIEM[SIEM/SOAR]
    ERP[ERP/CRM/HR]
  end
  Integrations --- LDAP & SSO & SMTP & MON & SIEM & ERP
```

## External Systems

- Identity: LDAP/AD, SAML/OIDC (SSO)
- Email: SMTP (outbound), IMAP/POP3 (inbound ticket creation)
- Monitoring: Nagios, Zabbix, custom webhooks
- Security: SIEM/SOAR (alerts, incident routing)
- Business Apps: ERP, CRM, HRIS (approvals, org structure)
- Mobile Push: FCM/APNs via outbound egress

## Boundaries

- Product boundary: monolith with event-driven modules; API Gateway front door.
- Data boundary: PostgreSQL (OLTP), MinIO (objects), Redis (cache/session).
