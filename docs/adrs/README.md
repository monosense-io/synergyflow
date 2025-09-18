---
title: ADRs Overview
owner: Architect
status: Draft
last_updated: 2025-09-18
---

Purpose

- Capture significant architectural decisions with context, options, and consequences.

Process

- Propose → Review → Accept/Reject → Immutable history (do not rewrite accepted ADRs; add a new ADR to supersede).

Template (YAML front-matter + sections)

```md
---
adr: 000x
title: <Decision Title>
status: Proposed|Accepted|Rejected|Superseded
date: YYYY-MM-DD
owner: <Role or Name>
links:
  - <related documents>
---

Context
Decision
Rationale
Consequences
Alternatives Considered
Follow-up Tasks
```

Lifecycle

- Status changes require reviewer sign-off (Architect + impacted owners).
- Supersession: reference previous ADR and explain rationale.
