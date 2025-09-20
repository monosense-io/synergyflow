Status: Draft

# Story
As a knowledge engineer,
I want a Knowledge Article model with versioning and attachments,
so that content changes are tracked, reviewable, and safely publishable.

## Acceptance Criteria
1. Create/read/update KnowledgeArticle with fields: title, body (rich text), tags, owner, status (Draft/Review/Published), and currentVersion.
2. Versioning model stores each change as ArticleVersion with diff metadata; rollback restores prior version with audit.
3. Attachments supported with size/type limits, virus scan hook, and secure storage; linked per article/version.
4. Draft→Review→Publish workflow state transitions enforced; invalid transitions blocked with errors; all actions audit logged.
5. Events emitted: kb.article.created/updated/published, kb.article.rolledback; event schemas versioned.

## Tasks / Subtasks
- [ ] Data models: KnowledgeArticle, ArticleVersion, Attachment (AC: 1, 2, 3)
- [ ] CRUD endpoints with validation and status transitions (AC: 1, 4)
- [ ] Versioning and rollback service with audit (AC: 2, 4)
- [ ] Attachment upload pipeline (limits, scanning, storage) (AC: 3)
- [ ] Event emissions for lifecycle changes (AC: 5)

## Dev Notes
- PRD: docs/prd/02-itil-v4-core-features-specification.md (2.7 Knowledge Management)
- ADRs: 0001 event bus; 0004 event contract versioning; 0009 DB migration policy; 0011 cache (optional)
- Architecture: data-architecture.md (storage/indexing patterns); testing-architecture.md
- APIs: knowledge.yaml for article/version endpoints; users.yaml for ownership

## Testing
- Versioning and rollback behavior; attachments security and limits
- Status transition rules and audit immutability
- Event contract tests for kb.article.*

## Change Log
| Date       | Version | Description                                      | Author |
|------------|---------|--------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 05 seed #1               | PO     |

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

