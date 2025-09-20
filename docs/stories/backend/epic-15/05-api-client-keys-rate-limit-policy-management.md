Status: Draft

# Story
As an API product manager,
I want API client and key management with rate limit policies,
so that partner and internal consumers authenticate and are governed appropriately.

## Acceptance Criteria
1. ApiClient model with owner, contact, scopes, environments; ApiKey issuance/rotation/revocation with hashed storage and last‑used metadata.
2. RateLimitPolicy associated per client and/or route; supports quotas and bursts; enforcement signals surfaced to gateway (Epic 15.01).
3. Admin APIs for managing clients/keys/policies with RBAC; secrets never returned; copy‑once display on issuance.
4. Audit logs for key lifecycle; notifications on nearing quota and key expiry; webhooks optional.
5. Reports: usage per client, top endpoints, and throttling events; export CSV; permissions enforced.

## Tasks / Subtasks
- [ ] ApiClient/ApiKey/RateLimitPolicy schemas and migrations (AC: 1, 2)
- [ ] Key issuance/rotation/revocation flows with hashing and last‑used (AC: 1)
- [ ] Policy association and gateway enforcement hook (AC: 2)
- [ ] Admin APIs + RBAC + audit + notifications (AC: 3, 4)
- [ ] Usage/throttling reports and exports (AC: 5)

## Dev Notes
- PRD: docs/prd/05-integration-architecture.md (auth and governance)
- Architecture: gateway‑envoy.md; security‑architecture.md
- ADRs: 0007 gateway; 0010 Envoy Gateway; 0009 DB migrations; 0001 event bus
- APIs: system.yaml (clients/keys/policies)

## Testing
- Key lifecycle; policy enforcement; audit; usage reports; permissions

## Change Log
| Date       | Version | Description                                         | Author |
|------------|---------|-----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — API clients, keys & rate limits     | PO     |

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

