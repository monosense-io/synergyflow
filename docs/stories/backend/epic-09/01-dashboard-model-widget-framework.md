Status: Draft

# Story
As a product analytics owner,
I want a Dashboard and Widget model with sharing and role visibility,
so that users can build and share customizable dashboards safely.

## Acceptance Criteria
1. Data models: Dashboard (owner, title, layout, visibility, sharedWith), Widget (type, queryRef/config, refreshInterval, visibility).
2. CRUD APIs for dashboards/widgets with validation; layout supports grid positions and sizes; optimistic concurrency for layout updates.
3. Sharing: per-user/team sharing with role-based visibility; public link option with token (if enabled by policy).
4. Permissions: row/field-level enforcement in widget queries via a central policy evaluator; unauthorized data is never returned.
5. Audit and versioning: dashboard layout/version history; revert to previous; all changes logged.

## Tasks / Subtasks
- [ ] Define models and migrations (Dashboard, Widget) (AC: 1)
- [ ] CRUD + layout validation + optimistic concurrency (AC: 2)
- [ ] Sharing model (users/teams) and tokenized public links (AC: 3)
- [ ] Policy integration for row/field-level security (AC: 4)
- [ ] Audit trail + version history + revert (AC: 5)

## Dev Notes
- PRD: docs/prd/02-itil-v4-core-features-specification.md (2.10 Dashboards)
- Architecture: data-architecture.md; api-architecture.md; security-architecture.md for policy enforcement
- ADRs: 0011 cache (optional for layout fetch); 0007 gateway policies
- APIs: system.yaml (dashboards) and/or search.yaml for data queries

## Testing
- CRUD and layout concurrency tests; sharing and token link security
- Policy enforcement tests (row/field-level) on sample queries
- Versioning/revert behavior and audit immutability

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — Dashboard/Widget model & APIs   | PO     |

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

