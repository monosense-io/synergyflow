Status: Draft

# Story
As an end user and admin,
I want a digest configuration UI,
so that periodic summaries are tailored to needs without causing noise.

## Acceptance Criteria
1. User can enable/disable digests per topic and choose frequency/time; admins can define org defaults and enforce for roles.
2. Preview shows sample digest content for selected topics/timeframe; localized per preference.
3. Quiet hours honored; critical overrides explained; conflict validation prevents overlapping noisy schedules.
4. Accessibility and performance standards met; save/get < 400ms.
5. Audit of changes and enforcement visibility (e.g., org‑enforced settings read‑only).

## Tasks / Subtasks
- [ ] Digest settings form (user) and admin default controls (AC: 1)
- [ ] Preview integration with backend composer (AC: 2)
- [ ] Quiet hours and conflict validation UX (AC: 3)
- [ ] a11y/perf checks; audit indicators (AC: 4, 5)

## Dev Notes
- Backend: docs/stories/backend/epic-11/04-dedupe-grouping-digest-service.md and 02‑preferences‑subscriptions‑apis.md
- PRD: 07 notifications — digests and preferences
- Architecture/UX: docs/ux/README.md; gateway‑envoy.md
- APIs: system.yaml (digest settings)

## Testing
- Save/load flows; preview correctness; enforcement UI; a11y/perf

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — digest configuration UI      | PO     |

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

