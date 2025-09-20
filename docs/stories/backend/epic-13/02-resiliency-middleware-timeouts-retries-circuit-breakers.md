Status: Draft

# Story
As a platform engineer,
I want standardized resiliency middleware (timeouts, retries with jitter, circuit breakers),
so that cross‑service calls degrade gracefully and protect upstreams.

## Acceptance Criteria
1. Provide library/middleware for outbound calls with configurable timeouts, retry policies (exponential backoff + jitter), and circuit breakers.
2. Default policies per integration class (internal service, external API, datastore) with overrides; failure budgets per dependency.
3. Emit metrics/traces for attempts, successes, failures, retries, and breaker state; logs include correlation IDs.
4. Global kill‑switch and per‑route toggles via config/flags to disable retries or open/close breakers in incidents.
5. Gateway policy aligned where applicable (Envoy timeouts/retries) to ensure end‑to‑end consistency.

## Tasks / Subtasks
- [ ] Core middleware library and config schema (AC: 1)
- [ ] Policy presets and overrides + failure budgets (AC: 2)
- [ ] Instrumentation for metrics/traces/logs (AC: 3)
- [ ] Feature flags/kill‑switches and admin endpoints (AC: 4)
- [ ] Gateway policy mapping (Envoy) and docs (AC: 5)

## Dev Notes
- PRD: HA/reliability requirements — degradation and protection
- Architecture: gateway-envoy.md; observability-reliability.md
- ADRs: 0010 Envoy Gateway, 0006 partitioning (if async), 0011 cache (optional)
- APIs: system.yaml (admin toggles)

## Testing
- Unit/integration tests for retries/backoff/jitter; circuit breaker transitions
- Metrics/traces presence; flag toggles; gateway parity tests

## Change Log
| Date       | Version | Description                                        | Author |
|------------|---------|----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — resiliency middleware (timeouts/retries/CB) | PO  |

## Dev Agent Record

### Agent Model Used
<record at implementation time>

### Debug Log References
<links at implementation time>

### Completion Notes List
<notes at implementation time>

### File List
<files at implementation time>

## QA Results
<QA to fill>

