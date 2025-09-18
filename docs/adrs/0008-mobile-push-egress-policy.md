---
adr: 0008
title: Mobile Push Notifications Egress Policy
status: Accepted
date: 2025-09-03
owner: Security
links:
  - ../prd/09-high-availability-reliability-architecture.md
---

Decision

- Allow outbound egress to FCM/APNs; document proxies/firewalls; monitor delivery.

Rationale

- Required for push notifications in on‑prem deployments.

Consequences

- Maintain allowlists; monitor for outages; provide fallback notification channels.
