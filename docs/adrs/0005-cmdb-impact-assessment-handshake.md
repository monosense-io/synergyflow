---
adr: 0005
title: CMDB Impact Assessment via Event Handshake
status: Accepted
date: 2025-09-03
owner: Architect
links:
  - ../prd/03-system-architecture-monolithic-approach.md
---

Decision

- Change publishes ChangeImpactAssessmentRequestedEvent; CMDB responds with ChangeImpactAssessedEvent.

Rationale

- Preserve zero-dependency policy while enabling synchronous-like gating via eventual consistency.

Consequences

- Approval flow must await assessment event (with timeout/retry paths).
