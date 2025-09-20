Status: Draft

# Story
As an integration engineer,
I want a webhook subscription service with signing, retries, and delivery tracking,
so that partners receive reliable, secure event notifications.

## Acceptance Criteria
1. WebhookSubscription model with callback URL, topics, secret, retry policy, status; verification challenge on create/update.
2. Delivery pipeline signs payloads (HMAC/PK) with timestamp and id; consumers can verify; clock skew tolerated.
3. Retries with exponential backoff, jitter, and max attempts; DLQ for permanent failures; idempotency keys prevent duplicates.
4. Delivery status APIs provide recent deliveries with filters; events emitted for success/failure; notifications for repeated failures.
5. Security: URL allowlist/denylist; payload filtering by topics; rate limiting per subscription; audit all changes and deliveries.

## Tasks / Subtasks
- [ ] Subscription CRUD + verification challenge (AC: 1)
- [ ] Signing + headers + timestamp/nonce verification (AC: 2)
- [ ] Retry/DLQ/idempotency pipeline (AC: 3)
- [ ] Delivery status APIs + events + notifications (AC: 4)
- [ ] Security policies (allow/deny/rate) + audit (AC: 5)

## Dev Notes
- PRD: docs/prd/05-integration-architecture.md (webhooks/events)
- Architecture: gateway‑envoy.md; api-architecture.md
- ADRs: 0004 contract versioning (event payloads); 0006 event partitioning; 0011 cache (optional)
- APIs/Events: events.yaml; system.yaml (subscriptions)

## Testing
- Subscription verification; signature verification; retry/DLQ behaviors; idempotency; rate limiting; audit and notifications

## Change Log
| Date       | Version | Description                                     | Author |
|------------|---------|-------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — webhook subscription service   | PO     |

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

