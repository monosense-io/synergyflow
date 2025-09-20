Status: Draft

# Story
As an end user,
I want knowledge search with highlighted previews and the ability to rate usefulness,
so that I can quickly find helpful content and improve results.

## Acceptance Criteria
1. Search UI provides query, filters, and sort; results show highlighted snippets and metadata (last updated, owner, tags).
2. Opening a result records a view; rating controls (useful/not useful or stars) submit a rating per user per version.
3. Related articles are shown based on keywords/category; clicking navigates accordingly.
4. Accessibility: keyboard navigation, proper semantics; responsive layout.
5. Performance: input→first results p95 < 600ms after debounced query.

## Tasks / Subtasks
- [ ] Search UI with filters/sort and debounced queries (AC: 1, 5)
- [ ] Result cards with highlighted snippets and metadata (AC: 1)
- [ ] View/rate actions wired to backend; rating state per user (AC: 2)
- [ ] Related articles panel (AC: 3)
- [ ] a11y and responsive checks (AC: 4)

## Dev Notes
- Backend dependency: docs/stories/backend/epic-03/03-knowledge-search-previews-ratings.md
- PRD: 2.5 Portal — knowledge search, rating
- Architecture/UX: docs/ux/README.md
- APIs: knowledge.yaml, search.yaml

## Testing
- Query behavior, filters, snippet highlighting
- Rating idempotency; view events
- a11y testing; performance timings

## Change Log
| Date       | Version | Description                                   | Author |
|------------|---------|-----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — knowledge search UI (Epic 03) | PO     |

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

