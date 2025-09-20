Status: Draft

# Story
As an end user and admin,
I want Preferences and Subscription APIs,
so that users can control channels, topics, quiet hours, and localization.

## Acceptance Criteria
1. Preference model per user: locale, time zone, quiet hours, per‑channel toggles (email/push/in‑app), severity thresholds, and digest settings.
2. Subscription model: per‑topic/category (e.g., incidents, changes, approvals) with channel overrides and frequency; org defaults supported.
3. APIs: get/update preferences (self), admin manage org defaults and user overrides; RBAC enforced; rate‑limit updates.
4. preference.updated event emitted on changes; audit entries record who/when/what changed.
5. Backfill/migration sets sane defaults for existing users; preferences respected by delivery pipeline.

## Tasks / Subtasks
- [ ] Data models: Preference, Subscription, OrgDefault (AC: 1, 2)
- [ ] Self‑service and admin APIs with RBAC and rate limits (AC: 3)
- [ ] Emit preference.updated and write audit logs (AC: 4)
- [ ] Migration/backfill job and default rules (AC: 5)
- [ ] Integrate preference checks into delivery routing (AC: 5)

## Dev Notes
- PRD: docs/prd/07-notification-system-design.md (preferences & localization)
- ADRs: 0001 event bus; 0006 partitioning; 0007 gateway for policy routes
- Architecture: security-architecture.md (RBAC); data-architecture.md
- APIs: docs/api/modules/system.yaml (preferences), docs/api/modules/users.yaml

## Testing
- Preference/Subscription CRUD; RBAC and rate limits
- Event contract for preference.updated; migration/backfill correctness
- Delivery honors preferences and quiet hours; digest flags influence

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — preferences & subscriptions  | PO     |

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

