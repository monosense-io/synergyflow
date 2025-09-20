Status: Draft

# Story
As an end user and agent,
I want high-performance knowledge search with previews and usefulness ratings,
so that I can quickly find and evaluate relevant articles.

## Acceptance Criteria
1. Search API returns paginated results with relevance scoring and preview snippets (highlighted matches), p95 < 800ms.
2. Endpoint supports filters (category, tags), sorting, and typo tolerance; protects against injection.
3. kb.viewed event emitted on article open; kb.rated on rating submit; ratings stored per user/article.
4. Abuse protections on ratings (one rating per user per version; update allowed).
5. Results include related articles suggestions based on category/keywords.

## Tasks / Subtasks
- [ ] Search index integration and query API with snippets (AC: 1)
- [ ] Filters/sorting/typo tolerance; input validation (AC: 2)
- [ ] Events for view/rate; rating API with limits (AC: 3, 4)
- [ ] Related articles suggestions endpoint (AC: 5)
- [ ] Performance tuning and caching (AC: 1)

## Dev Notes
- PRD: 2.5 Portal (integrated knowledge search and ratings)
- Dependencies: Knowledge Management (Epic 05)
- Architecture: data-architecture.md; caching via ADR 0011 (DragonflyDB)
- ADRs: 0001 event bus; 0004 versioning; 0011 cache store
- APIs: knowledge.yaml, search.yaml

## Testing
- Relevance and snippet generation tests
- Ratings limits and update behavior
- Event contracts for kb.viewed/kb.rated
- Performance tests for p95 latency

## Change Log
| Date       | Version | Description                                  | Author |
|------------|---------|----------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 03 seed #3           | PO     |

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

