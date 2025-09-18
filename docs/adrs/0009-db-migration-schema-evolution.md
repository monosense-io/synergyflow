---
adr: 0009
title: DB Migration & Schema Evolution Policy
status: Accepted
date: 2025-09-03
owner: DBA/Architect
links:
  - ../../migrations/
---

Decision
- Versioned, idempotent migrations (V###); forward-only preferred; rollback notes included.

Rationale
- Predictable deployments; reproducible environments; clear audit trail.

Consequences
- Change management discipline; review gates for performance and data safety.

