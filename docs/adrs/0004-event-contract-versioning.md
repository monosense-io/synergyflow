---
adr: 0004
title: Event Contract Versioning Strategy
status: Accepted
date: 2025-09-03
owner: Architect
links:
  - ../event-catalog.md
---

Decision

- Version events; favor additive changes; route by version if needed; deprecate with overlap.

Rationale

- Preserve backward compatibility while evolving schemas.

Consequences

- Consumers must tolerate unknown fields; sunset plan required.
