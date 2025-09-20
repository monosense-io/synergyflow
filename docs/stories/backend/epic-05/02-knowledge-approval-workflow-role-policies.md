Status: Draft

# Story
As a content reviewer and knowledge admin,
I want a governed approval workflow with role policies,
so that only reviewed content is published and audit-ready.

## Acceptance Criteria
1. Review tasks are generated on Draft→Review transition; approvers determined by role policy (owner team/KB admins).
2. Reviewers can approve or request changes with rationale; all actions audit logged.
3. Publish is only permitted after required approvals; unauthorized publish attempts are blocked.
4. Notifications sent to reviewers/owners on review requested/approved/changes requested; preferences respected.
5. Events: kb.review.requested, kb.review.approved, kb.review.changes_requested, kb.article.published.

## Tasks / Subtasks
- [ ] Role policy configuration and evaluator (AC: 1)
- [ ] Review task lifecycle management (AC: 1, 2)
- [ ] Publish gate enforcing approvals (AC: 3)
- [ ] Notification hooks to Epic 11 (AC: 4)
- [ ] Event emissions for review lifecycle (AC: 5)

## Dev Notes
- PRD: 2.7 Knowledge Management — approval workflows
- Dependencies: Security & RBAC (Epic 12); Notifications (Epic 11)
- ADRs: 0001 event bus; 0004 versioning
- Architecture: workflow considerations in testing-architecture.md for review flows
- APIs: knowledge.yaml (review endpoints); users.yaml for roles; system.yaml for preferences

## Testing
- Role policy selection; review lifecycle; publish gate enforcement
- Notification delivery tests; audit checks
- Event contract tests

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 05 seed #2               | PO     |

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

