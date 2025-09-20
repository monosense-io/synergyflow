Status: Draft

# Story
As a service desk agent and system integrator,
I want incident lifecycle management with comprehensive audit logging,
so that incident state changes are traceable, compliant, and easy to troubleshoot.

## Acceptance Criteria
1. Create, read, update, and transition incidents across the full lifecycle with role-based permissions.
2. Every status transition writes an immutable audit record including user, timestamp, action, previous/new values, and optional note.
3. Incident creation supports attachments and metadata from multiple channels (email/portal/mobile), normalized into a single model.
4. Audit records are queryable by incident ID and exportable for compliance reports.
5. Event emissions occur for ticket.created and ticket.updated; schemas follow event contract versioning.

## Tasks / Subtasks
- [ ] Model incident lifecycle states and transitions (AC: 1)
  - [ ] Define allowed transitions and role policies (AC: 1)
- [ ] Implement audit logging pipeline (AC: 2, 4)
  - [ ] Persist immutable audit events with signatures/IDs (AC: 2)
  - [ ] Add query/export endpoints (AC: 4)
- [ ] Multi-channel ingestion normalization (AC: 3)
  - [ ] Email ingestion adapter → normalized Incident DTO (AC: 3)
  - [ ] Portal API ingestion → normalized Incident DTO (AC: 3)
  - [ ] Mobile ingestion → normalized Incident DTO (AC: 3)
- [ ] Event publication (AC: 5)
  - [ ] ticket.created, ticket.updated emission with versioned schema (AC: 5)
- [ ] RBAC enforcement on transitions (AC: 1)

## Dev Notes
- PRD alignment: docs/prd/02-itil-v4-core-features-specification.md (2.1 Incident Management: multi-channel, templates, SLA, audit logging)
- ADRs: 
  - Event-only comms: docs/adrs/0001-event-only-inter-module-communication.md
  - Event contract versioning: docs/adrs/0004-event-contract-versioning.md
  - DB migration policy: docs/adrs/0009-db-migration-schema-evolution.md
- Architecture:
  - Eventing: docs/architecture/eventing-architecture.md
  - API: docs/architecture/api-architecture.md
  - Module boundaries/testing: docs/prd/24-spring-modulith-architecture-compliance-summary.md
- APIs to touch: docs/api/modules/incidents.yaml, docs/api/modules/events.yaml, docs/api/modules/users.yaml
- Data: Incident, Attachment, AuditLog; consider monotonic timestamps for ordering; include request source channel.

## Testing
- Follow docs/architecture/testing-architecture.md and docs/prd/11-testing-strategy-spring-modulith.md
- Contract tests for ticket.created/updated events against versioned schema
- Lifecycle transition tests (happy-path, unauthorized, invalid transitions)
- Audit immutability and export tests

## Change Log
| Date       | Version | Description                                 | Author |
|------------|---------|---------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft from Epic 01 seed #1          | PO     |

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

