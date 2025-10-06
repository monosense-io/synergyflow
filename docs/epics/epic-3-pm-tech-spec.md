# Epic 3: Core PM Module - Technical Specification

**Epic:** Epic 3 — Core PM Module
**Project:** SynergyFlow (Enterprise ITSM & PM Platform)
**Author:** monosense (Architect)
**Date:** 2025-10-06
**Timeline:** Weeks 3–10 (8 weeks, parallel with Epic 2-ITSM)
**Team:** 2 BE + 1 FE (Team B - PM)
**Dependencies:** Epic 1 (Foundation) complete; runs parallel with Epic 2 (ITSM)
**Project Level:** Level 4 (Platform/Ecosystem)

---

## Overview

Epic 3 delivers the Core PM module: issues, Kanban boards, sprints, backlog, and metrics with selective “board intelligence” enhancements. It aligns with the Modulith + Companions architecture, using read models for hot paths and publishing domain events for realtime UI and search/correlation.

## Objectives and Scope

In scope (v1):
- Issue CRUD (Story/Task/Bug/Epic) with history and comments.
- Boards with drag-and-drop, configurable columns, WIP limits; multiple boards per project.
- Sprints, backlog, assignment with capacity validation; burndown.
- Search/filters; basic metrics (velocity, CFD, throughput, cycle time).
- Innovations (MVP): Card Intelligence (ambiguity score + estimate reality check), Board Command Palette.

Out of scope (v1):
- Full Auto‑PR scaffolding flow to external Git providers (stub hook only).
- Contract‑First Guard enforcement across repos (stub OpenAPI generator + “blocked” banner only).
- Advanced analytics and cross‑project portfolio views.

## System Architecture Alignment

- Package root: `io.monosense.synergyflow.pm`
- Modulith modules used: `pm` (this epic), `security`, `eventing`, `sse`, `audit`.
- Data: OLTP tables for PM domain; denormalized read models: `issue_card`, `board_view`, `sprint_summary`.
- Events: `IssueCreated`, `IssueStateChanged`, `IssueLinked`, `SprintUpdated` → outbox → stream → read models + SSE.
- Realtime: SSE gateway pushes board/metrics deltas; idempotent client reducers by `{id, version}`.

---

## Detailed Design

### Services and Modules

- API (`pm/api`)
  - `IssueController` — CRUD, search, linking, comments.
  - `BoardController` — board config, DnD move, WIP limits.
  - `SprintController` — create/update sprint, assign issues, burndown.
  - `MetricsController` — velocity, CFD, throughput, cycle time.

- SPI (`pm/spi`)
  - `IssueQueryService` — cross‑module queries from dashboard/workflow.

- Internal (`pm/internal`)
  - Domain: `Issue`, `Sprint`, `Board`, `BoardColumn`, `IssueLink`, `IssueComment`.
  - Repositories: JPA repos for above; read‑model repos.
  - Services: `IssueService`, `BoardService`, `SprintService`, `MetricsService`, `CardIntelligenceService`.
  - Projection: `IssueCardProjection`, `BoardViewProjection`, `SprintSummaryProjection`.
  - Events: `IssueCreatedEvent`, `IssueStateChangedEvent`, `IssueLinkedEvent`, `SprintUpdatedEvent`.

### Data Models and Contracts

Core OLTP (DDL sketch):
```sql
CREATE TABLE issues (
  id UUID PRIMARY KEY,
  type VARCHAR(16) NOT NULL,            -- Story|Task|Bug|Epic
  title TEXT NOT NULL,
  description TEXT,
  status VARCHAR(16) NOT NULL,          -- Backlog|ToDo|InProgress|InReview|Done
  priority VARCHAR(16),
  assignee_id UUID,
  reporter_id UUID NOT NULL,
  story_points INTEGER,
  sprint_id UUID,
  epic_id UUID,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL,
  version BIGINT NOT NULL
);

CREATE TABLE sprints (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  start_date DATE NOT NULL,
  end_date DATE NOT NULL,
  goal TEXT,
  capacity_points INTEGER
);

CREATE TABLE boards (
  id UUID PRIMARY KEY,
  name TEXT NOT NULL,
  project_id UUID NOT NULL
);

CREATE TABLE board_columns (
  id UUID PRIMARY KEY,
  board_id UUID NOT NULL REFERENCES boards(id),
  name TEXT NOT NULL,
  position INTEGER NOT NULL,
  wip_limit INTEGER
);

CREATE TABLE issue_links (
  id UUID PRIMARY KEY,
  src_issue_id UUID NOT NULL,
  dst_issue_id UUID NOT NULL,
  relation VARCHAR(16) NOT NULL       -- blocks|blocked_by|relates|duplicates|child_of
);

CREATE TABLE issue_comments (
  id UUID PRIMARY KEY,
  issue_id UUID NOT NULL,
  author_id UUID NOT NULL,
  text TEXT NOT NULL,
  is_internal BOOLEAN DEFAULT false,
  created_at TIMESTAMPTZ NOT NULL
);
```

Read models (denormalized):
- `issue_card(issue_id, title, type, status, priority, assignee, sprint, due_in, updated_at, version)`
- `board_view(board_id, column_name, position, issue_cards[])`
- `sprint_summary(sprint_id, points_total, points_done_by_day[])`

### APIs and Interfaces

OpenAPI stubs under `docs/api/` define initial contracts.

Key endpoints (PM):
- `GET /api/pm/issues` — list/search (filters: type,status,assignee,reporter,sprint,epic,labels,q).
- `POST /api/pm/issues` — create.
- `GET /api/pm/issues/{id}` — fetch.
- `PUT /api/pm/issues/{id}` — update fields; optimistic by `If-Match: version`.
- `POST /api/pm/issues/{id}/comments` — add comment (with @mentions).
- `POST /api/pm/issues/{id}/links` — create link (blocks/relates/duplicates/child_of).
- `GET /api/pm/boards/{boardId}` — board view (columns + issue cards).
- `POST /api/pm/boards/{boardId}/move` — move issue: `{issueId, fromColumn, toColumn, position}`; enforces WIP.
- `GET /api/pm/sprints/{sprintId}/burndown` — points remaining by day.
- `GET /api/pm/metrics/velocity` — last N sprints velocity.

Cross‑module SPI:
- `IssueQueryService.findIssueCards(ids[])` for dashboard/workflow bundles.

### Workflows and Sequencing

1) Backlog → Sprint planning → Board execution.
2) Drag issue to column → `IssueStateChangedEvent` → outbox → read models update → SSE push.
3) Card Intelligence runs on issue changes and board focus; surfaces ambiguity and estimate checks.
4) Optional hooks (stub in v1):
   - Auto‑PR scaffolding on transition to In Progress.
   - Contract‑First Guard for API‑tagged issues (generate OpenAPI stub; block until merged).

---

## Non-Functional Requirements

### Performance
- List/board p95 <200ms; side‑panel hydrate p95 <300ms; CRUD p95 <400ms; p99 <800ms.
- Payload discipline: issue card ≤6KB typical; board view paged by column.

### Security
- OIDC JWT validation; RBAC with project/team scopes; server‑side scope filters.
- Comments and internal notes separated; audit all state transitions.

### Reliability/Availability
- 99.9% uptime target; graceful degradation (read models stale <2s p95); retry idempotency.

### Observability
- Tracing: issue CRUD, board move, search; metrics: p95/p99 latencies, SSE reconnects, reducer drops; logs: state transitions, WIP violations.

---

## Dependencies and Integrations
- Keycloak (OIDC/JWT), PostgreSQL 16+, Redis 7 (Streams), OpenSearch (optional until S8–S9).
- Git provider (stub): outbound webhook + token config for Auto‑PR scaffolding.

---

## Acceptance Criteria (Authoritative)
1. Create/read/update/close issues with full history and comments (FR‑PM‑1,8,11).
2. Board shows columns with drag‑and‑drop; state updates persist; WIP limits enforced (FR‑PM‑2,12).
3. Create sprints; assign issues; capacity validation prevents over‑allocation (FR‑PM‑3,4).
4. Backlog view supports priority reorder and assignment to sprints (FR‑PM‑5).
5. Burndown shows ideal vs actual; updates within 2s of events (FR‑PM‑6).
6. Search filters by keyword, type, status, assignee, reporter, sprint, epic, labels, date range (FR‑PM‑10).
7. Issue linking supports blocks/blocked_by/relates/duplicates/child_of (FR‑PM‑9).
8. Metrics dashboard shows velocity, CFD, throughput, cycle time (FR‑PM‑14).
9. Card Intelligence surfaces ambiguity score and estimate check on board focus (MVP) (FR‑PM‑16).
10. Command palette (⌘K) provides listed quick actions (FR‑PM‑20).

---

## Traceability Mapping (Excerpt)

| FR | Component/API | Read Model | Test Idea |
|---|---|---|---|
| PM‑2 | `BoardController.move`, SSE | `board_view` | Drag card → state persisted, SSE delta <2s |
| PM‑6 | `SprintController.burndown` | `sprint_summary` | Daily points drop matches done issues |
| PM‑16 | `CardIntelligenceService` | n/a | Ambiguity score appears; edit AC inline |

---

## Risks, Assumptions, Open Questions
- Risk: Holiday capacity lowers output in S6 → mitigate by hardening focus.
- Risk: WIP enforcement causing user friction → add override with reason + audit.
- Assumption: Git provider access not guaranteed in v1 → keep Auto‑PR as stub.
- Question: Board sharing model across projects? Default to per‑project boards.

---

## Test Strategy Summary
- Unit: domain/service logic (>80% for domain).
- Integration: CRUD + WIP rules + search (Testcontainers: Postgres/Redis).
- Contract: OpenAPI for PM endpoints; ETag/If‑Match behavior.
- E2E: Board DnD, backlog, sprint planning (Playwright).
- Performance: Gatling scenarios for list/board; SSE reconnect/replay.
