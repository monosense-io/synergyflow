Status: Draft

# Story
As a notifications product owner,
I want deduplication, grouping, and digest services,
so that users avoid alert fatigue while staying informed.

## Acceptance Criteria
1. Dedupe policies configurable per event/topic/channel (e.g., suppress identical notifications within N minutes); policy evaluation audited.
2. Grouping aggregates similar notifications within a time window into a single message (e.g., multiple SLA warnings); respects localization and personalization.
3. Digest scheduler compiles periodic summaries (hourly/daily) per user/topic; digest content is localized and respects preferences.
4. Preferences (quiet hours, channel enablement) always respected; overrides allowed for critical severity per policy.
5. Metrics: suppression rate, grouping effectiveness, digest opt‑in rate; APIs expose metrics; events for digest.dispatched.

## Tasks / Subtasks
- [ ] Dedupe policy engine and storage; evaluation hooks (AC: 1)
- [ ] Grouping aggregator and formatter with localization (AC: 2)
- [ ] Digest scheduler and composer; dispatch integration (AC: 3)
- [ ] Preference enforcement and critical overrides (AC: 4)
- [ ] Metrics collection and APIs; digest events (AC: 5)

## Dev Notes
- PRD: docs/prd/07-notification-system-design.md (dedupe, grouping, digests)
- ADRs: 0006 partitioning for burst handling; 0001 event bus; 0011 cache (optional for grouping windows)
- Architecture: data-architecture.md; observability-reliability.md (metrics, schedulers)
- APIs: system.yaml (metrics/endpoints)

## Testing
- Dedupe and grouping behaviors under various windows; localization correctness
- Digest composition and dispatch; preference and override handling
- Metrics exposure; event contracts

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — dedupe/grouping/digests      | PO     |

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

