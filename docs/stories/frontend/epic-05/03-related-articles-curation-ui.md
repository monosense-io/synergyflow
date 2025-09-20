Status: Draft

# Story
As a knowledge curator,
I want a related articles curation UI,
so that I can review, pin, or exclude suggestions to improve relevance.

## Acceptance Criteria
1. UI shows machine-generated related article suggestions with confidence; allows pin/exclude per source article.
2. Curations override algorithm results and are versioned with rationale; audit shows who/when/what.
3. Search/filter by article, tags, confidence; bulk pin/exclude actions for authorized users.
4. Preview shows how suggestions will appear in portal and agent UIs.
5. Accessibility and performance standards met.

## Tasks / Subtasks
- [ ] Suggestions list with confidence and actions; filters (AC: 1, 3)
- [ ] Pin/exclude persistence with versioning and audit (AC: 2)
- [ ] Bulk actions and permissions (AC: 3)
- [ ] Preview panel rendering (AC: 4)
- [ ] a11y/perf checks (AC: 5)

## Dev Notes
- Backend: docs/stories/backend/epic-05/04-related-articles-suggestion-service.md
- PRD: 2.7 suggestions
- Architecture/UX: docs/ux/README.md; gateway-envoy.md
- ADRs: 0007 gateway
- APIs: knowledge.yaml (suggestions/curations)

## Testing
- Curation overrides; filters; audit trails
- Permission tests; preview rendering

## Change Log
| Date       | Version | Description                                         | Author |
|------------|---------|-----------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — related articles curation UI (05)   | PO     |

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

