Status: Draft

# Story
As an end user and agent,
I want related article suggestions during ticket creation and updates,
so that I can discover helpful content without searching manually.

## Acceptance Criteria
1. Suggestion service provides related articles given context (query, category, tags, ticket text) with configurable strategy (content similarity, co-click, tags).
2. API returns top-N suggestions with confidence; excludes Draft; respects permissions.
3. Performance: p95 < 400ms; results cached with TTL and invalidation on article changes.
4. Telemetry captures click-through and effectiveness for tuning.
5. Events: kb.suggestion.served, kb.suggestion.clicked (for analytics).

## Tasks / Subtasks
- [ ] Similarity algorithm implementation and strategy switcher (AC: 1)
- [ ] Suggestion API with filters/permissions (AC: 2)
- [ ] Caching and invalidation on article updates (AC: 3)
- [ ] Telemetry capture for CTR/effectiveness (AC: 4)
- [ ] Event emissions for analytics (AC: 5)

## Dev Notes
- PRD: 2.7 Knowledge Management — related article suggestions
- ADRs: 0011 cache; 0001 event bus; 0004 versioning
- Architecture: data-architecture.md (similarity options)
- APIs: knowledge.yaml (suggestions), search.yaml

## Testing
- Suggestion relevance tests; filter/permission correctness
- Cache behavior and invalidation
- Event and telemetry capture

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 05 seed #4               | PO     |

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

