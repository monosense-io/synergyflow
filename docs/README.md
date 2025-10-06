# SynergyFlow Documentation

This folder contains the working documentation set for the SynergyFlow (Enterprise ITSM & PM Platform) initiative.

---

## Conventions

- File names: kebab-case, lowercase (e.g., `prd-validation-report.md`).
- Header fields (when applicable):
  - `Project: SynergyFlow (Enterprise ITSM & PM Platform)`
  - `Author`, `Date`, `Status`, `Version`, `Epic`, `Timeline`, `Team`.
- Use consistent role titles (e.g., Architect, Product Manager).

---

## Index

- Product:
- `product/prd.md` — Product Requirements (v3.0)
- `product/prd-validation-report.md` — Validation report against PRD v3.0

Epics:
- `epics/epic-1-foundation-tech-spec.md`
- `epics/epic-2-itsm-tech-spec.md`
- `epics/epic-3-pm-tech-spec.md`
- `epics/epic-4-workflow-tech-spec.md`

Architecture:
- `architecture/architecture-validation-report.md` — Backend architecture validation (status: warnings)
- `architecture/architecture-blueprint.md` — C4 context/container/deployment + runtime views
- `architecture/solution-architecture.md` — Consolidated solution architecture overview
- `architecture/gradle-build-config.md` — Build configuration reference

Planning:
- `planning/project-plan.md` — Project plan and milestones
- `planning/sprint-plan.md` — Detailed sprint schedule and goals
- `planning/team-allocation-matrix.md` — Roles, teams, and RACI
- `planning/risk-register.md` — Top risks and mitigations
- `planning/dependency-map.md` — Epic sequencing and gates

API specs (stubs):
- `api/itsm-api.yaml`
- `api/pm-api.yaml`
- `api/workflow-api.yaml`
- `api/sse-api.yaml`
- `api/README.md` — Versioning, auth, cursor/replay semantics

Architecture details:
- `architecture/module-boundaries.md` — Allowed dependencies, @NamedInterface catalog, enforcement
- `architecture/data-model.md` — ERD and migration pointers
- `architecture/db/migrations/V1__core_schema.sql` — Initial Flyway schema (docs)
- `architecture/eventing-and-timers.md` — Outbox, streams, idempotency, SLA timers
- `architecture/performance-model.md` — SLOs, test plan, gates
- `architecture/epic-alignment-matrix.md` — Mapping of epics to components, APIs, and read models

ADRs:
- `architecture/adr/0001-modulith-and-companions.md`
- `architecture/adr/0002-uuidv7.md`
- `architecture/adr/0003-redis-streams-vs-kafka.md`
- `architecture/adr/0004-sse-vs-websocket.md`

Reports & Research:
- `project-workflow-analysis.md` — Workflow and planning analysis
- `brainstorming-session-results-2025-10-05.md` — Ideation outcomes
- `uuidv7-implementation-guide.md` — UUIDv7 guidance and options
- `corrections-summary.md` — What changed in the last alignment
