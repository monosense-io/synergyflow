Status: Draft

# Story
As a knowledge admin,
I want expiry notifications and review workflows for stale articles,
so that content remains current and trustworthy.

## Acceptance Criteria
1. Scheduler detects articles approaching or past expiry based on policy (per tag/category/owner) and queues review tasks.
2. Notifications sent to content owners and reviewers with links to review; preferences and localization applied.
3. Review outcomes: renew (extend), update (new draft/version), or retire; all actions audited.
4. Events: kb.article.expiring, kb.article.renewed, kb.article.retired; expiring digest option available.
5. Performance: scheduler scales to 100k articles with partitioned scans and backoff; observability metrics exposed.

## Tasks / Subtasks
- [ ] Expiry policy model (global/override by tag/category) (AC: 1)
- [ ] Scheduler and partitioned scanning (AC: 1, 5)
- [ ] Review task creation and outcomes (AC: 3)
- [ ] Notifications and digest capability (AC: 2, 4)
- [ ] Events and observability metrics (AC: 4, 5)

## Dev Notes
- PRD: 2.7 Knowledge Management — expiry notifications and review
- ADRs: 0006 event partitioning for scale; 0001 event bus; 0004 versioning
- Architecture: observability-reliability.md (metrics), data-architecture.md
- APIs: knowledge.yaml (review tasks), users.yaml (owners), system.yaml (notifications)

## Testing
- Policy evaluation; scheduler correctness and performance
- Notification content and preference handling; digest groupings
- Review outcomes and audit

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 05 seed #5               | PO     |

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

