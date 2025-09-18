---
adr: 0002
title: Only 'user' as Shared Foundation Module
status: Accepted
date: 2025-09-03
owner: Architect
links:
  - ../prd/03-system-architecture-monolithic-approach.md
---

Decision
- 'user' is the only shared module; 'team' depends on 'user'; others use events only.

Rationale
- Minimize coupling; maintain clear boundaries.

Consequences
- Requires projections/read models for cross-module reads.

