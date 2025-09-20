Status: Draft

# Story
As a platform administrator,
I want a normalized data model and APIs for teams, agent profiles, skills, and capacity,
so that routing can operate on accurate structure and availability data.

## Acceptance Criteria
1. Data models: Team (org→dept→team hierarchy), AgentProfile (user ref, timezone, certifications), Skill (name, level), Capacity (current load, caps, business hours ref).
2. CRUD APIs for Team, AgentProfile, Skill; capacity read/update endpoints publish capacity.updated events on change.
3. Team membership and skill matrices maintained for agents; validations prevent orphaned members and invalid skill levels.
4. Timezone stored per agent; business hours reference resolvable (will be defined in a separate story); availability computed stub provided.
5. RBAC enforced: only admin roles can mutate teams/skills/capacity; audit logs recorded for all writes.

## Tasks / Subtasks
- [ ] Define ORM/DB schemas and migrations (Team, AgentProfile, Skill, Capacity) (AC: 1)
- [ ] CRUD endpoints and validators; membership management (AC: 2, 3)
- [ ] Capacity update endpoint and capacity.updated event (AC: 2)
- [ ] RBAC and auditing for write operations (AC: 5)
- [ ] Timezone + availability computation stub integration (AC: 4)

## Dev Notes
- PRD: docs/prd/04-multi-team-support-intelligent-routing.md (team hierarchy, skills, availability)
- ADRs: 0002 user as shared foundational module; 0006 event partitioning; 0001 event bus; 0009 DB migration policy
- Architecture: module-architecture.md; data-architecture.md; api-architecture.md
- APIs: docs/api/modules/teams.yaml, docs/api/modules/users.yaml
- Events: capacity.updated

## Testing
- CRUD validations and membership integrity
- Capacity update emits event and persists state
- RBAC enforcement and audit immutability

## Change Log
| Date       | Version | Description                                          | Author |
|------------|---------|------------------------------------------------------|--------|
| 2025-09-18 | 0.1     | Initial draft — teams/agents/skills/capacity model   | PO     |

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

