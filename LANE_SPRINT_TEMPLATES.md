# 🎯 Lane Sprint Templates — SynergyFlow 24-Sprint Plan

> **Templates optimized for Lane project management system** | Ready to import/adapt

---

## 📍 Table of Contents

- [Quick Start: How to Use](#quick-start-how-to-use)
- [Sprint Template Structure](#sprint-template-structure)
- [Phase 1 Complete Breakdown (Sprints 01–04)](#phase-1-complete-breakdown-sprints-0104)
- [Phase 2 Complete Breakdown (Sprints 05–12)](#phase-2-complete-breakdown-sprints-0512)
- [Phase 3 Complete Breakdown (Sprints 13–22)](#phase-3-complete-breakdown-sprints-1322)
- [Phase 4 Complete Breakdown (Sprints 23–24)](#phase-4-complete-breakdown-sprints-2324)
- [Swimlane Cards Template](#swimlane-cards-template)
- [Story Card Template](#story-card-template)
- [Acceptance Criteria Templates](#acceptance-criteria-templates)
- [WIP Limit Monitoring Sheet](#wip-limit-monitoring-sheet)
- [CSV Import Format](#csv-import-format)

---

## Quick Start: How to Use

### Option A: Copy-Paste Into Lane (Markdown)
1. Go to Lane board
2. Create new Epic/Sprint
3. Copy entire section (e.g., "Phase 1 Sprint 01")
4. Paste into Lane description/notes
5. Create individual story cards from breakdown

### Option B: Use CSV (Bulk Import)
1. Go to **CSV Import Format** section
2. Copy CSV table
3. Import into Lane via bulk import feature
4. Stories auto-create with metadata

### Option C: Manual Card Creation
1. Use **Story Card Template** (copy structure)
2. Create one card per story in Lane
3. Assign to lane (Backend, Frontend, DevOps, QA)
4. Link dependencies via Lane's link feature

### Option D: Lane Board View (Copy-Paste Swimlanes)
1. Use **Swimlane Cards Template**
2. Create columns per lane
3. Paste story card structure into each column
4. Move cards across columns as you work

---

## Sprint Template Structure

### Standard Sprint Header (Copy This)

```
# Sprint NN: [Epic Name] — Phase [Number] (Weeks X–Y)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks (Weeks X–Y) |
| **Phase** | Phase N: [Phase Name] |
| **Main Epic(s)** | Epic-XX [Name] |
| **Parallel Epics** | Epic-YY, Epic-ZZ (if any) |
| **Primary Focus** | [Key deliverable] |
| **Exit Criteria** | [3-4 measurable criteria] |

## 🎯 Goals
- Goal 1: [Specific outcome]
- Goal 2: [Specific outcome]
- Goal 3: [Specific outcome]

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: X/3]
| Story | Title | Points | Owner | Status | Notes |
|-------|-------|--------|-------|--------|-------|
| XX.01 | [Title] | 13 | @engineer-1 | Ready | [Priority/blocker info] |
| XX.02 | [Title] | 8 | @engineer-2 | Blocked By: XX.01 | [Dependency] |

### 🎨 Frontend (1 engineer) — [WIP: X/2]
| Story | Title | Points | Owner | Status | Notes |
|-------|-------|--------|-------|--------|-------|
| XX.03 | [Title] | 5 | @frontend | Ready | Contract locked? Yes |

### 🚀 DevOps (1 engineer) — [WIP: X/2]
| Story | Title | Points | Owner | Status | Notes |
|-------|-------|--------|-------|--------|-------|
| XX.04 | [Title] | 8 | @devops | Ready | Parallel with all |

### 🧪 QA (shared ~0.5 FTE) — [WIP: X/2]
| Story | Title | Points | Owner | Status | Notes |
|-------|-------|--------|-------|--------|-------|
| Test | [Test Suite] | 5 | @qa | Ready | Depends on XX.01 |

## 📊 Lane Board View
```
🔧 BACKEND          🎨 FRONTEND         🚀 DEVOPS           🧪 QA
───────────────     ───────────────     ───────────────     ───────────────
[xx.01] Title       [xx.03] Title       [xx.04] Title       [Test] Suite
Story pts: 13       Story pts: 5        Story pts: 8        Test pts: 5
Owner: eng1         Owner: fe           Owner: devops       Owner: qa
Status: Ready       Status: Ready       Status: Ready       Status: Ready
───────────────     ───────────────     ───────────────     ───────────────
[xx.02] Title       [xx.05] Title       [xx.06] Title       [Test] Suite 2
Story pts: 8        Story pts: 8        Story pts: 5        Test pts: 8
Owner: eng2         Owner: fe           Owner: devops       Owner: qa
Status: Blocked     Status: Queued      Status: Ready       Status: Queued
   By: xx.01
```

---
```

---

## Phase 1 Complete Breakdown (Sprints 01–04)

### Sprint 01: Platform Foundation Kickoff (Weeks 1–2)

```
# Sprint 01: Platform Foundation Kickoff — Phase 1 (Weeks 1–2)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 1: Foundation |
| **Main Epic** | Epic-16: Platform Foundation |
| **Primary Focus** | Spring Modulith baseline, PostgreSQL, CI/CD skeleton |
| **Exit Criteria** | Modulith event bus online, DB reachable, CI green on skeletons, WIP limits enforced |

## 🎯 Goals
- Initialize Spring Modulith 1.4.2 with event registry
- Deploy PostgreSQL + PgBouncer shared cluster
- Establish CI/CD skeleton with pipeline gates
- Verify all lanes can deploy independently

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: 1/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 16.05 | Spring Modulith Config | 13 | @backend-1 | ✅ ApplicationModules#verify() passes ✅ Event publish/consume working ✅ Modulith boundaries enforced ✅ No unintended cross-module deps |

**16.05 Details:**
- Create `modulith-config` module
- Define Modulith boundaries (Incident, Change, User, Knowledge, Catalog, Routing)
- Wire ApplicationEvents listener pattern
- Document module contracts
- Acceptance test validates event flow across modules

### 🎨 Frontend (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| (Reserve) | Next.js Bootstrap Verification | 3 | @frontend | ✅ App runs locally ✅ Tailwind + shared tokens loaded ✅ TypeScript strict mode passes |

**Details:**
- Verify Next.js 15 app bootstrap (already done in prior work)
- Check UI token imports
- No new feature; supporting story only

### 🚀 DevOps (1 engineer) — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 16.10 | PostgreSQL + PgBouncer | 13 | @devops | ✅ PgBouncer running ✅ Dev/Stg/Prod schemas created ✅ JDBC pool 20–100 connections ✅ Hikari config in JPA ✅ Connection health check passes |
| 16.14 | CI/CD Workflows Skeleton | 8 | @devops | ✅ GitHub Actions workflow runs on push ✅ Build passes ✅ Unit tests gated ✅ Manual promotion gates for Stg/Prod |

**16.10 Details:**
- Deploy PgBouncer in dev environment
- Create 3 Postgres databases (dev, stg, prod)
- Configure per-module schemas
- Wire Hikari connection pooling

**16.14 Details:**
- GitHub Actions: compile, unit test, SonarQube scan
- Promotion gates: manual approval for Stg and Prod
- Artifact storage (Docker images, JAR)

### 🧪 QA (shared ~0.5 FTE) — [WIP: 2/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| CI | Modulith Smoke Tests | 5 | ✅ `ApplicationModules#verify()` runs in CI ✅ Event flow test passes ✅ Pipeline fails if modules invalid |
| CI | DB Connectivity Smoke | 3 | ✅ Hikari pool test passes ✅ Schema creation validated ✅ Connection timeout tests included |

## 📊 Lane Board Snapshot
```
🔧 BACKEND              🎨 FRONTEND              🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[16.05] Modulith        [Reserve] FE Boot        [16.10] PgBouncer       [CI] Modulith
Pts: 13                 Pts: 3                   Pts: 13                 Pts: 5
Owner: be1              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: READY            Status: READY           Status: READY
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
                                                 [16.14] CI/CD           [CI] DB Smoke
                                                 Pts: 8                  Pts: 3
                                                 Owner: devops           Owner: qa
                                                 Status: READY           Status: READY
```

## 📌 Dependencies & Blockers
- 16.10 (DB) → unblocks 16.05 testing
- 16.14 (CI) → gates all future PRs
- FE bootstrap → supports 16.05 testing (no blocking)

## ⚠️ Risks
| Risk | Probability | Mitigation |
|------|-------------|-----------|
| Modulith config complexity | Medium | Pre-read docs; schedule architecture review Week 0 |
| PgBouncer pooling issues | Low | Load test pooling in parallel with 16.05 |
| CI/CD gates too strict | Low | Test with dummy PR first |

---

### Sprint 02: Platform Continuation (Weeks 3–4)

```
# Sprint 02: Platform Foundation Continuation — Phase 1 (Weeks 3–4)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 1: Foundation |
| **Main Epic** | Epic-16: Platform Foundation |
| **Primary Focus** | DragonflyDB cache, Observability (Grafana + metrics), GitOps (Flux CD) |
| **Exit Criteria** | Dashboards live, dev env deploys from main, module scaffolds compile |

## 🎯 Goals
- Deploy DragonflyDB cache cluster
- Standup Victoria Metrics + Grafana dashboards
- Bootstrap Flux CD GitOps with dev/stg/prod overlays
- Define SLO dashboard templates for latency, errors, event lag

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: 1/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 16.11 | DragonflyDB Cache Config | 13 | @backend-2 | ✅ Cache abstraction layer in utils ✅ Dragonfly pool connections stable ✅ TTL policies enforced ✅ Cache hit/miss metrics tracked |

**16.11 Details:**
- Spring Data Redis abstraction
- Dragonfly connection pooling (10–50 connections)
- Cache invalidation policies (TTL, manual evict)
- Metrics export (hit ratio, evictions)
- Performance benchmarks: cache lookup <5ms p99

### 🎨 Frontend (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| Support | Telemetry Integration | 5 | @frontend | ✅ API client logs requests/responses ✅ Error boundaries render ✅ Trace IDs propagated ✅ No console errors from instrumentation |

**Details:**
- Wire OpenTelemetry to Next.js API client
- Error boundary component catches React errors
- Correlation IDs in request headers

### 🚀 DevOps (1 engineer) — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 16.12 | Observability Stack | 13 | @devops | ✅ Victoria Metrics running ✅ Grafana dashboards for latency, errors, event lag ✅ OpenTelemetry exporter configured ✅ Alert rules defined (latency >500ms, error rate >1%) |
| 16.13 | GitOps Deployment (Flux CD) | 13 | @devops | ✅ Flux CD bootstrap complete ✅ Dev overlay deploys from main branch ✅ Secrets managed via SOPS ✅ Infrastructure-as-Code committed to git |

**16.12 Details:**
- Victoria Metrics: time-series storage
- Grafana dashboards: latency (p50/p95/p99), error rate, event lag
- OpenTelemetry SDK configured
- Alert notification channels (Slack)

**16.13 Details:**
- Flux CD gitOps bootstrap
- Kustomize overlays: dev → stg → prod
- SOPS for secrets in git
- Automated reconciliation enabled

### 🧪 QA (shared ~0.5 FTE) — [WIP: 2/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| Obs | Dashboard SLO Probes | 5 | ✅ Grafana dashboards load in <2s ✅ Latency metrics update every 10s ✅ Error rate card populated |
| Ops | Cache Hit/Miss Validation | 3 | ✅ Cache hit ratio >70% on repeated queries ✅ Eviction logs present ✅ Memory usage under 512MB |

## 📊 Lane Board Snapshot
```
🔧 BACKEND              🎨 FRONTEND              🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[16.11] Dragonfly       [Support] Telemetry      [16.12] Obs Stack       [Obs] Dashboard
Pts: 13                 Pts: 5                   Pts: 13                 Pts: 5
Owner: be2              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: READY            Status: READY           Status: READY
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
                                                 [16.13] Flux CD         [Ops] Cache
                                                 Pts: 13                 Pts: 3
                                                 Owner: devops           Owner: qa
                                                 Status: READY           Status: READY
```

## 📌 Dependencies & Blockers
- 16.12 (Obs) → enables monitoring for all future work
- 16.13 (GitOps) → enables auto-deployments
- 16.11 (Cache) → ready anytime (no dependencies)

## ⚠️ Risks
| Risk | Probability | Mitigation |
|------|-------------|-----------|
| Flux CD learning curve | Medium | Schedule 1-day Flux workshop Week 2 |
| Grafana dashboard complexity | Low | Use dashboard templates from community |
| Cache eviction tuning | Low | Load test with 10x QPS spike |

---
```

### Sprint 03: Trust + UX Foundation Part 1 (Weeks 5–6)

```
# Sprint 03: Trust + UX Foundation (Part 1) — Phase 1 (Weeks 5–6)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 1: Foundation |
| **Main Epic** | Epic-00: Trust + UX Foundation |
| **Primary Focus** | Event System (transactional outbox), Link-on-Action scaffolding |
| **Exit Criteria** | Events persisted + delivered p95 <200ms, cross-module link seed flows working, contracts validated |

## 🎯 Goals
- Implement transactional outbox pattern for event durability
- Build Link-on-Action infrastructure (bidirectional link persistence)
- Validate event latency p95 <200ms (target SLA)
- Complete contract specifications for event schemas

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: 2/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 00.01 | Event System + Transactional Outbox | 21 | @backend-1 | ✅ ApplicationEvents#publish() stores to outbox table ✅ Outbox poller drains events async ✅ Event delivery p95 <200ms ✅ No duplicate events (idempotency key) ✅ Outbox schema migrated via Flyway |
| 00.03(BE) | Link Infrastructure | 13 | @backend-2 | ✅ Bidirectional link entity created ✅ Link creation cascades properly ✅ Cross-module link queries work ✅ Link deletion cascades safe ✅ Link audit trail persisted |

**00.01 Details:**
- Create `events` table: (id, aggregate_type, aggregate_id, event_type, payload, created_at)
- Create `outbox` table: (id, event_id, processed_at, retries)
- Scheduled job: poll outbox every 100ms, publish to event bus, mark processed
- Idempotency: events are idempotent by (aggregate_id, event_type)
- Dead-letter queue for failed events

**00.03(BE) Details:**
- Link entity: (id, source_id, source_type, target_id, target_type, created_at)
- Service: LinkService#createLink(), #deleteLink(), #findLinksFor(entity)
- Cross-module call via event: e.g., IncidentLinkedToChange event

### 🎨 Frontend (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 00.03(FE) | Link-on-Action UX | 8 | @frontend | ✅ "Link to" affordance visible in context menus ✅ Link modal shows available entities ✅ Link creation feedback (toast) ✅ Bidirectional link visible on both entities |

**00.03(FE) Details:**
- Context menu: "Link to Incident/Change/Article"
- Modal form: search + select target entity
- Call API: POST /api/links
- Render linked entities below entity details

### 🚀 DevOps (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| OPA | OPA Sidecar Setup (Shadow Mode) | 13 | @devops | ✅ OPA sidecar container running ✅ Policies injected from ConfigMap ✅ Decision logs captured ✅ Correlation IDs in logs ✅ Policy evaluation latency <50ms p99 |

**Details:**
- Deploy OPA as sidecar in dev environment
- Policies: shadow mode (log decisions, don't enforce)
- Log enrichment: add correlation IDs, request ID
- Latency monitoring enabled

### 🧪 QA (shared ~0.5 FTE) — [WIP: 1/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| Contracts | Event Schema Contracts | 8 | ✅ AsyncAPI spec for all event types ✅ Event payload examples valid ✅ Compatibility tests pass ✅ Contract tests in CI |
| Contracts | Link Endpoint Contracts | 5 | ✅ POST /api/links request/response documented ✅ Link payload examples valid ✅ OpenAPI spec complete |

## 📊 Lane Board Snapshot
```
🔧 BACKEND              🎨 FRONTEND              🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[00.01] Outbox          [00.03] Link UX          [OPA] Shadow Setup      [Contracts] Events
Pts: 21                 Pts: 8                   Pts: 13                 Pts: 8
Owner: be1              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: READY            Status: READY           Status: READY
Blocked: No             Contract Locked: Yes     Blocked: No
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[00.03] Link Infra      [00.03] Link Feedback                            [Contracts] Links
Pts: 13                 (included above)                                 Pts: 5
Owner: be2              Status: READY                                    Owner: qa
Status: READY                                                            Status: READY
```

## 📌 Dependencies & Blockers
- 00.01 (Outbox) → **CRITICAL PATH** blocks all future events
- 00.03 (Links) → can develop in parallel (independent domain)
- OPA → runs independently (shadow mode non-blocking)

## ⚠️ Risks & Mitigations
| Risk | Probability | Mitigation |
|------|-------------|-----------|
| Outbox latency >200ms | Medium | Performance test Week 1; tune polling interval if needed |
| Link deletion cascades | Low | Database constraints tested; foreign keys strict |
| OPA policy complexity | Low | Start with 1 simple policy; expand incrementally |

---

### Sprint 04: Trust + UX Foundation Part 2 (Weeks 7–8)

```
# Sprint 04: Trust + UX Foundation (Part 2) — Phase 1 (Weeks 7–8)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 1: Foundation |
| **Main Epic** | Epic-00: Trust + UX Foundation |
| **Primary Focus** | Time Tray MVP, Freshness Badges, Policy Studio (shadow), Decision Receipts |
| **Exit Criteria** | 100% policy evals emit decision receipts, freshness badges visible, OPA shadow deployment validated |

## 🎯 Goals
- Implement Time Tray for work-on-entry mirroring
- Build Freshness Badge UI component + backend staleness detection
- Standup Policy Studio + decision receipt persistence
- Validate policy decision receipts for audit trail

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: 2/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 00.02 | Time Tray MVP | 21 | @backend-1 | ✅ Mirror worklog entries from work item creation ✅ Time Tray API returns all mirrors ✅ Refresh logic handles deletions ✅ Timestamps accurate to second ✅ Concurrent updates safe |
| 00.05(BE) | Decision Receipts | 13 | @backend-3 | ✅ Receipt schema: policy_id, decision (allow/deny), evidence, timestamp ✅ 100% of policy evals logged ✅ Receipts queryable by entity ✅ Audit trail immutable (no deletes) |

**00.02 Details:**
- Mirror table: (id, source_id, source_type, created_at, refreshed_at)
- Service: TimeTrayService#createMirror(), #refreshMirrors()
- API: GET /api/time-tray?filter=week
- Automatic cleanup of stale mirrors (>30 days)

**00.05(BE) Details:**
- Receipt entity: policy_id, target_entity_id, decision, evidence_json, created_at
- Event: PolicyEvaluated → creates receipt
- Receipts stored in immutable table
- No business logic on receipts (audit-only)

### 🎨 Frontend (1 engineer) — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 00.04 | Freshness Badges | 8 | @frontend | ✅ Badge renders on entity detail page ✅ Colors indicate staleness: green (<1h), yellow (1–4h), red (>4h) ✅ Hover shows "Last updated X minutes ago" ✅ Threshold configurable per entity type |
| 00.05(FE) | Policy Studio UI | 13 | @frontend | ✅ Shadow mode toggle visible ✅ Decision receipt viewer shows policy + evidence ✅ Receipt history searchable ✅ Receipts export to CSV |

**00.04 Details:**
- Badge component: Badge(staleness: 'fresh'|'stale'|'very_stale')
- Colors defined in Tailwind tokens
- Refresh button triggers backend update
- Threshold configurable in admin settings

**00.05(FE) Details:**
- Shadow mode toggle in navbar
- Receipt viewer: timeline of decisions
- Filter by entity type, decision (allow/deny)
- CSV export via API

### 🚀 DevOps (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| OPA | OPA Production Rollout Patterns | 13 | @devops | ✅ OPA policies deployed via CI/CD ✅ Policy versioning tracked in git ✅ Rollback automation works (revert + restart) ✅ Policy update latency <30s ✅ No traffic loss during updates |

**Details:**
- Terraform: OPA ConfigMap management
- CI/CD: policy linting (OPA fmt, regal)
- Rollback: git revert → auto-redeploy
- Monitoring: policy evaluation success rate >99.9%

### 🧪 QA (shared ~0.5 FTE) — [WIP: 2/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| E2E | Time Tray E2E Flow | 8 | ✅ Create incident → mirror appears in Time Tray ✅ Modify time entry → mirror updates ✅ Concurrent updates safe (no races) |
| E2E | Policy Receipt Validation | 5 | ✅ Policy evaluation → receipt created ✅ Receipt immutable (no updates) ✅ Receipt audit trail queryable |

## 📊 Lane Board Snapshot
```
🔧 BACKEND              🎨 FRONTEND              🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[00.02] Time Tray       [00.04] Badges           [OPA] Rollout           [E2E] Time Tray
Pts: 21                 Pts: 8                   Pts: 13                 Pts: 8
Owner: be1              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: READY            Status: READY           Status: READY
Blocked: 00.01 done     Blocked: BE 00.02 done   Blocked: No
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[00.05] Receipts        [00.05] Studio           [Monitoring] Setup       [E2E] Receipts
Pts: 13                 Pts: 13                  (Support Task)           Pts: 5
Owner: be3              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: Ready            Status: Ready           Status: READY
```

## 📌 Dependencies & Blockers
- 00.02 (Time Tray) → depends on event system (00.01) ✅ ready
- 00.05 (Receipts) → depends on OPA setup (Sprint 03) ✅ ready
- All stories independent (can parallelize)

## ⚠️ Phase 1 Exit Gate Checklist
- [ ] Modulith event bus online (Story 16.05)
- [ ] PostgreSQL + PgBouncer reachable (Story 16.10)
- [ ] DragonflyDB cache operational (Story 16.11)
- [ ] Grafana dashboards live (Story 16.12)
- [ ] Flux CD deploys automatically (Story 16.13)
- [ ] CI/CD pipeline green (Story 16.14)
- [ ] Events delivered p95 <200ms (Story 00.01)
- [ ] Outbox pattern validated (Story 00.01)
- [ ] Links bidirectional (Story 00.03)
- [ ] Time Tray MVP working (Story 00.02)
- [ ] Freshness Badges visible (Story 00.04)
- [ ] Policy decision receipts immutable (Story 00.05)
- [ ] OPA shadow mode operational (Story 00.05)
- [ ] All contracts validated (QA)
- [ ] No critical bugs open (QA)

**🟢 PHASE 1 GATE PASS** → Ready for Phase 2 (Incidents + Changes)

---
```

---

## Phase 2 Complete Breakdown (Sprints 05–12)

### Sprint 05: Incident Management Part 1 (Weeks 9–10)

```
# Sprint 05: Incident Management (Part 1) — Phase 2 (Weeks 9–10)

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 2: Core Features |
| **Main Epic** | Epic-01: Incident Management |
| **Primary Focus** | Incident CRUD, lifecycle, SLA tracking (Flowable timers), comments/attachments |
| **Exit Criteria** | Incident workflows demoable end-to-end, SLA timers within ±5s p95, audit log complete |

## 🎯 Goals
- Implement Incident domain (CRUD + lifecycle state machine)
- Build Flowable SLA timer integration
- Create comments + attachments infrastructure
- Wire incident events to event bus

## 🏊 Swimlane Allocation

### 🔧 Backend (3 engineers) — [WIP: 3/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 01.01 | Incident CRUD + Classification | 21 | @backend-1 | ✅ Incident entity: title, description, priority (P1–P4), severity (Critical–Low) ✅ CRUD API fully functional ✅ Validation: title required, priority valid enum ✅ Audit log tracks creator, creation_time |
| 01.03 | SLA Timer Tracking (Flowable) | 21 | @backend-2 | ✅ SLA thresholds defined per priority ✅ Flowable processes start on incident creation ✅ Timer fires based on configured delays ✅ Pre-breach notifications triggered at 80% of SLA ✅ Timers ±5s accurate (p95) |
| 01.04 | Incident Lifecycle Workflow | 13 | @backend-3 | ✅ States: Open → Assigned → In Progress → Resolved → Closed ✅ State transitions validat ✅ Lifecycle events published (Created, Assigned, Resolved) ✅ User role permissions enforced per state |

**01.01 Details:**
- Entity: Incident (id, title, description, priority, severity, status, created_at, created_by)
- API: GET /incidents, POST /incidents, PATCH /incidents/{id}
- Validation: title 10–200 chars, priority enum, severity enum
- Audit: created_by, updated_by tracked

**01.03 Details:**
- SLA config: P1=1h, P2=4h, P3=8h, P4=24h
- Flowable process definition: SLATimer.bpmn20.xml
- Timer event: user notification at 80% SLA elapsed
- Database: incident_sla table tracks start_time, deadline, notified_at, breached_at

**01.04 Details:**
- State machine: Open → Assigned → In Progress → Resolved → Closed
- Transitions: only valid role can transition (e.g., only assignee can move to In Progress)
- Events: IncidentCreated, IncidentAssigned, IncidentResolved
- Saga: ensure linked problems also updated

### 🎨 Frontend (1 engineer) — [WIP: 1/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 01.UI | Incident List + Detail Pages | 8 | @frontend | ✅ List page: table with incident ID, title, priority, status, assigned_to ✅ Sorting by priority, created_at ✅ Detail page: full incident info + state buttons ✅ Comments section stub ✅ Responsive design (mobile-friendly) |

**Details:**
- List page: React table with sorting/filtering
- Detail page: incident info card + state machine buttons
- Status badge colors: Open=blue, Assigned=yellow, In Progress=orange, Resolved=green
- Comments stub: ready for Sprint 05 additions

### 🚀 DevOps (1 engineer) — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 01.Alerts | SLA Alerting Setup | 13 | @devops | ✅ Grafana alert rule: incident SLA >80% elapsed ✅ Alert routes to on-call Slack channel ✅ Pre-breach alert fires 30 min before deadline ✅ Alert includes incident ID + priority ✅ No alert spam (dedupe) |
| 01.Storage | Attachment Storage Infrastructure | 8 | @devops | ✅ S3 bucket created (dev/stg/prod) ✅ File size limit: 50MB per attachment ✅ Access control: only incident owner + assignee ✅ Virus scan integration ready |

**Details:**
- Grafana: alert rule on incident_sla metric
- Slack integration via webhook
- S3: separate folders per incident
- Virus scan: ClamAV lambda trigger on upload

### 🧪 QA (shared ~0.5 FTE) — [WIP: 2/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| WebMvc | Incident CRUD Unit Tests | 8 | ✅ Create incident: valid + invalid inputs tested ✅ Update incident: state transitions validated ✅ Delete incident: cascade behavior tested |
| E2E | Happy Path: Create → Assign | 8 | ✅ Create incident via UI ✅ Search for incident ✅ Assign to team member ✅ SLA timer visible in detail ✅ Incident appears in list |

## 📊 Lane Board Snapshot
```
🔧 BACKEND              🎨 FRONTEND              🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[01.01] CRUD            [01.UI] List/Detail      [01.Alerts] SLA         [WebMvc] CRUD
Pts: 21                 Pts: 8                   Pts: 13                 Pts: 8
Owner: be1              Owner: fe                Owner: devops           Owner: qa
Status: READY           Status: READY            Status: READY           Status: READY
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[01.03] SLA Timers      [Comments Stub]          [01.Storage] S3          [E2E] Happy Path
Pts: 21                 (in UI story)            Pts: 8                  Pts: 8
Owner: be2              Status: Ready            Owner: devops           Owner: qa
Status: READY                                    Status: Ready           Status: Ready
─────────────────────   ─────────────────────    ─────────────────────   ─────────────────
[01.04] Lifecycle       [Status Badge Colors]    [Monitoring] Setup
Pts: 13                 (in UI story)            (Support Task)
Owner: be3              Status: Ready            Owner: devops
Status: READY                                    Status: Ready
```

## 📌 Dependencies & Blockers
- All backend stories independent (can parallelize)
- FE depends on BE API contract (locked Week 8)
- DevOps: alerting depends on metrics pipeline (Sprint 02) ✅ ready

---

[Similar breakdown continues for Sprint 06–12...]

```

---

## Phase 3 Complete Breakdown (Sprints 13–22)

[Abbreviated for space; follows same template structure]

### Sprint 09: Portal vs. Catalog Split (CRITICAL)

```
# Sprint 09: Self-Service Portal + Catalog START (Weeks 17–18) — Phase 2 [SPLIT TEAM]

## 📋 Overview
| Aspect | Details |
|--------|---------|
| **Duration** | 2 weeks |
| **Phase** | Phase 2: Core Features (Weeks 17–24, Sprints 09–12) |
| **Main Epic(s)** | Epic-03 (Portal) ✕ Epic-04 (Catalog) — **PARALLEL** |
| **Team Split** | 🎨 FE=Portal | 🔧 BE (2 engineers)=Catalog |
| **Primary Focus** | Portal SSO + Preferences ⊕ Catalog Domain Model + Schema Engine |
| **Exit Criteria** | SSO flow working, Catalog CRUD API validated, contracts locked, split team executing safely |

## ⚠️ CRITICAL PARALLELISM WINDOW
```
┌─────────────────────────────────────────────────┐
│ SPRINT 09–12: SPLIT TEAM EXECUTION              │
├─────────────────────────────────────────────────┤
│                                                 │
│ 🎨 FRONTEND (1 engineer)  →  PORTAL             │
│    ├─ Story 03.01: SSO + Prefs (Sprint 09)      │
│    ├─ Story 03.02: Form engine (Sprint 10)      │
│    ├─ Story 03.03: Knowledge search (Sprint 11) │
│    └─ Story 03.04–05: Status + A11y (Sprint 12) │
│                                                 │
│ 🔧 BACKEND (2 engineers)  →  CATALOG            │
│    ├─ Story 04.01: CRUD + lifecycle (Sprint 09) │
│    ├─ Story 04.02: Schema engine (Sprint 10)    │
│    ├─ Story 04.03: Approval flows (Sprint 11)   │
│    └─ Story 04.04–05: Fulfillment (Sprint 12)   │
│                                                 │
│ 🚀 DEVOPS & 🧪 QA (shared)                       │
│    ├─ IdP integration (Keycloak/Auth0)          │
│    ├─ API gateway policies                      │
│    ├─ Feature flags (for Portal)                │
│    └─ E2E tests (Portal + Catalog workflows)    │
│                                                 │
│ SYNCHRONIZATION POINTS:                        │
│    • End of Sprint 08: Contract specs locked    │
│    • Mid-Sprint 09: API stubs ready for FE      │
│    • End of Sprint 10: Integration testing      │
│    • End of Sprint 12: Both stories feature-complete
│                                                 │
└─────────────────────────────────────────────────┘
```

## 🏊 Swimlane Allocation

### 🎨 FRONTEND (1 engineer) — PORTAL FOCUS — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 03.01 | Portal SSO + User Preferences | 21 | @frontend | ✅ SAML/OIDC login flow works ✅ JWT token stored securely ✅ User preferences saved (theme, language) ✅ Logout clears tokens ✅ Session timeout after 30 min idle |
| 03.02(partial) | Guided Ticket Submission | 13 | @frontend | ✅ Dynamic form renders from schema ✅ Field validation on blur ✅ Required field indication ✅ Form submission calls API ✅ Success/error toast feedback |

**03.01 Details:**
- SAML redirect to Keycloak
- JWT token: access_token (15 min) + refresh_token (7 days)
- localStorage: tokens + user_id
- Preferences: theme, language, default view
- Logout: clear localStorage, revoke token

**03.02 Details:**
- Form renderer: Input, Select, Textarea, Checkbox based on schema
- Validation: regex, required, min/max length
- Error display: under field
- Submit: POST /api/tickets with form data

### 🔧 BACKEND (2 engineers) — CATALOG FOCUS — [WIP: 3/3]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| 04.01 | Service CRUD + Lifecycle | 21 | @backend-1 | ✅ Service entity: name, description, category, owner, status (draft/active/archived) ✅ CRUD API complete ✅ Validation: name required, category from list ✅ Owner: team or individual ✅ Search by name/category |
| 04.02 | Dynamic Form Schema Engine | 21 | @backend-2 | ✅ Schema storage: form_id, schema_json, version ✅ Schema validation: fields required, types valid ✅ API: GET /schemas/{form_id} ✅ Schema versions immutable (no updates, only new versions) ✅ Default values configurable |

**04.01 Details:**
- Service entity: (id, name, description, category_id, owner_id, status, created_at)
- API: GET /services, POST /services, PATCH /services/{id}, DELETE /services/{id}
- Search: full-text on name + description
- Lifecycle: only owner can delete; archive instead

**04.02 Details:**
- Schema JSON: { "fields": [ { "name": "email", "type": "email", "required": true }, ... ] }
- Schema validation: Ajv (JSON schema validator)
- Versioning: schema_version increments on save
- Default values: { "email": "user@company.com" } per environment

### 🚀 DEVOPS (1 engineer) — [WIP: 2/2]
| Story | Title | Points | Owner | Acceptance Criteria |
|-------|-------|--------|-------|---------------------|
| IdP | Keycloak IdP Configuration | 13 | @devops | ✅ Keycloak instance running ✅ SAML realm created ✅ Service provider (app) registered ✅ SAML metadata exchanged ✅ JWT token generation working |
| GW | API Gateway Portal Policies | 8 | @devops | ✅ Unauthenticated routes: /auth/login, /health ✅ Authenticated routes: /api/* (require JWT) ✅ Portal path: /portal/* (require JWT + Portal role) ✅ Rate limit: 100 req/min per user ✅ CORS allowed for app domain |

**Details:**
- Keycloak: Helm chart deploy
- SAML: ACS URL, entity ID configured
- JWT: HS256 signing with shared secret
- Gateway: Envoy filters for auth + rate limit

### 🧪 QA (shared ~0.5 FTE) — [WIP: 2/2]
| Task | Title | Points | Acceptance Criteria |
|------|-------|--------|---------------------|
| Auth | SSO E2E Flow | 8 | ✅ Navigate to /login ✅ Redirect to Keycloak ✅ Login with test user ✅ Redirect to app ✅ JWT token in browser storage ✅ User profile loaded |
| Contracts | Schema + Service API Contracts | 8 | ✅ OpenAPI spec: POST /services, POST /schemas ✅ Request/response examples valid ✅ Error responses documented (400, 401, 500) ✅ Contract tests in CI |

## 📊 Lane Board Snapshot
```
🎨 FRONTEND (PORTAL)    🔧 BACKEND (CATALOG)    🚀 DEVOPS               🧪 QA
─────────────────────   ─────────────────────   ─────────────────────   ─────────────────
[03.01] SSO             [04.01] Service CRUD    [IdP] Keycloak          [Auth] E2E SSO
Pts: 21                 Pts: 21                 Pts: 13                 Pts: 8
Owner: fe               Owner: be1              Owner: devops           Owner: qa
Status: READY           Status: READY           Status: READY           Status: READY
Contract: Locked        Contract: Locked        Blocked: No             Blocked: No
─────────────────────   ─────────────────────   ─────────────────────   ─────────────────
[03.02] Form Engine     [04.02] Schema Engine   [GW] Portal Policies    [Contracts] APIs
Pts: 13                 Pts: 21                 Pts: 8                  Pts: 8
Owner: fe               Owner: be2              Owner: devops           Owner: qa
Status: READY           Status: READY           Status: READY           Status: READY
Blocked: 03.01 done     Blocked: 04.01 done     Blocked: IdP done       Blocked: No
```

## 📌 Dependencies & Blockers
- **Between lanes:** IdP (DevOps) → unblocks SSO (FE)
- **FE-Internal:** 03.01 (SSO) → 03.02 (forms) must complete in order
- **BE-Internal:** 04.01 (CRUD) → 04.02 (schema) must complete in order
- **Cross-lane:** Portal API stubs needed for FE (can use mock server Week 8)

## 🎯 Synchronization Points
| Point | Timing | Action |
|-------|--------|--------|
| **API Contracts Locked** | End Sprint 08 | BE provides OpenAPI specs to FE |
| **Mock Server Ready** | Week 17 (Sprint 09 start) | FE uses mock API while BE builds real APIs |
| **IdP Integration** | Week 17 (Sprint 09) | DevOps deploys Keycloak; FE integrates login |
| **Mid-Sprint Integration** | Week 18 (Sprint 09 end) | FE and BE APIs integrated; contract tests run |
| **Portal MVP** | Week 24 (Sprint 12 end) | Portal + Catalog fully integrated, feature-complete |

## ⚠️ Split Team Risks & Mitigations
| Risk | Probability | Mitigation |
|------|-------------|-----------|
| API contract mismatch | Medium | Lock contracts Week 8; mock server validation |
| FE blocks on BE API | High | Use mock server; implement contract tests |
| Coordination overhead | Medium | Daily standup (15 min); async Slack updates |
| FE context switching | High | **Pair FE + BE during crunch; reduce context switches** |
| Catalog scope creep | Medium | Strict story acceptance criteria; change control |

## 📋 Pre-Sprint 09 Checklist
- [ ] API contracts finalized and documented (OpenAPI)
- [ ] Mock server setup for FE (prism/mirage)
- [ ] Keycloak environment ready (auth endpoint ready)
- [ ] Catalog schema design approved (JSON schema examples)
- [ ] Portal routes defined (/portal/*, /api/services/*, etc.)
- [ ] Cross-lane standups scheduled (daily, 9 AM)
- [ ] Deployment target (stg) ready for integration tests

---
```

---

## Swimlane Cards Template

```markdown
# Swimlane Cards: [Sprint Name]

## 🔧 Backend Lane
```
Card 1:
┌─────────────────────────────────┐
│ Story: [Epic#.Story#]           │
│ Title: [Story Title]             │
│ Points: [8/13/21]               │
│ Assigned to: @engineer          │
│ Status: [ ] Ready [ ] In Progress│
│           [ ] Blocked [ ] Done   │
│ Dependencies:                    │
│ • Blocked By: [Story#] (if any) │
│ • Unblocks: [Story#] (if any)   │
│ Acceptance Criteria:             │
│ ✓ [AC1]                         │
│ ✓ [AC2]                         │
│ ✓ [AC3]                         │
└─────────────────────────────────┘
```

## 🎨 Frontend Lane
```
Card 1:
┌─────────────────────────────────┐
│ Story: [Epic#.Story#]           │
│ Title: [Story Title]             │
│ Points: [5/8/13]                │
│ Assigned to: @frontend          │
│ Status: [ ] Ready [ ] In Progress│
│           [ ] Blocked [ ] Done   │
│ Contract Locked: [ ] Yes [ ] No  │
│ Acceptance Criteria:             │
│ ✓ [AC1]                         │
│ ✓ [AC2]                         │
│ ✓ [AC3]                         │
└─────────────────────────────────┘
```

## 🚀 DevOps Lane
```
Card 1:
┌─────────────────────────────────┐
│ Task: [Infrastructure Task]      │
│ Title: [Task Title]              │
│ Points: [5/8/13]                │
│ Assigned to: @devops            │
│ Status: [ ] Ready [ ] In Progress│
│           [ ] Blocked [ ] Done   │
│ Environments: dev/stg/prod       │
│ Acceptance Criteria:             │
│ ✓ [AC1]                         │
│ ✓ [AC2]                         │
│ ✓ [AC3]                         │
└─────────────────────────────────┘
```

## 🧪 QA Lane
```
Card 1:
┌─────────────────────────────────┐
│ Task: [Test Suite/Contract]      │
│ Title: [Task Title]              │
│ Points: [3/5/8]                 │
│ Assigned to: @qa                │
│ Status: [ ] Ready [ ] In Progress│
│           [ ] Blocked [ ] Done   │
│ Depends on Stories:              │
│ • [Story#] [Status]             │
│ • [Story#] [Status]             │
│ Coverage:                        │
│ ✓ Unit tests: [%]              │
│ ✓ Integration: [%]             │
│ ✓ E2E: [%]                     │
└─────────────────────────────────┘
```

---
```

---

## Story Card Template

```markdown
# Story Card Template

## 📋 Header
**Story ID:** [Epic#.Story#]
**Title:** [Clear, action-oriented title]
**Phase:** Phase [N]: [Phase Name]
**Epic:** [Epic Name]
**Assigned Lane:** [ ] Backend [ ] Frontend [ ] DevOps [ ] QA

## 📊 Metrics
| Metric | Value |
|--------|-------|
| **Story Points** | 13 |
| **Sprint** | Sprint NN |
| **Estimated Hours** | 40 (1 engineer, 1 sprint) |
| **Priority** | 🔴 High / 🟡 Medium / 🟢 Low |
| **Status** | Backlog / Ready / In Progress / Blocked / Done |

## 📝 Description
[2–3 sentence summary of what needs to be built]

**Why:** [Business value or technical enabler]

**Acceptance Criteria:**
- [ ] AC1: [Specific, testable requirement]
- [ ] AC2: [Specific, testable requirement]
- [ ] AC3: [Specific, testable requirement]
- [ ] AC4: [Specific, testable requirement]

## 🔗 Dependencies
| Dependency | Type | Status |
|------------|------|--------|
| Story XX | Blocker | ✅ Done |
| Story YY | Enabler | 🔄 In Progress |
| API Contract ZZ | Locked | ✅ Yes |

## 📌 Technical Details
**Domain:** [Module name, if applicable]
**Database Changes:** [Yes/No; if yes, list tables]
**API Changes:** [New endpoints? Yes/No]
**UI Changes:** [New screens? Yes/No]
**Performance Impact:** [None / Low / Medium / High]

## ✅ Definition of Done
- [ ] Code written and reviewed (2 approvals)
- [ ] Unit tests pass (>80% coverage)
- [ ] Acceptance criteria validated
- [ ] Integration tests pass
- [ ] Documentation updated
- [ ] Deployed to dev/stg environments
- [ ] Product Owner sign-off

## 📚 References
- [Link to requirements doc]
- [Link to design/wireframes]
- [Link to related stories]
- [Link to test plan]

---
```

---

## Acceptance Criteria Templates

### Backend Story Template

```markdown
# Backend Story: [Title]

## Acceptance Criteria

### Functional
- [ ] Entity/Model: [Entity name] created with fields: [list]
- [ ] CRUD: Create, Read, Update, Delete operations work
- [ ] Validation: Invalid inputs rejected with proper error messages
- [ ] Persistence: Data stored in [table name]; schema migrated via [Flyway/Liquibase]
- [ ] Relationships: Links to [other entities] correctly established
- [ ] Events: [Event name] published on [trigger]
- [ ] Audit Trail: creator, creation_time, updater, update_time tracked
- [ ] Concurrency: Concurrent updates handled safely (optimistic locking)

### API Contracts
- [ ] OpenAPI spec updated: [endpoint list]
- [ ] Request schema validates (e.g., POST body example)
- [ ] Response schema validates (e.g., 200 response example)
- [ ] Error responses documented (400, 401, 403, 404, 500)
- [ ] Contract tests in CI pass

### Performance
- [ ] Query p99 latency <[X]ms (e.g., 100ms for list operations)
- [ ] Database indexes on commonly queried fields
- [ ] N+1 query problems avoided
- [ ] Caching configured (TTL, invalidation strategy)

### Security
- [ ] Authentication: Requests require valid JWT
- [ ] Authorization: Role-based access enforced ([roles list])
- [ ] Input sanitization: SQL injection, XSS prevented
- [ ] Sensitive data: PII fields masked in logs

---
```

### Frontend Story Template

```markdown
# Frontend Story: [Title]

## Acceptance Criteria

### UI/UX
- [ ] Component renders without errors
- [ ] Responsive design: works on mobile (320px), tablet (768px), desktop (1440px)
- [ ] Styling: follows Tailwind tokens + brand colors
- [ ] Dark mode: supported (if applicable)
- [ ] Loading states: spinners, skeletons shown while loading
- [ ] Empty states: user-friendly message when no data

### Functionality
- [ ] User interactions: click, input, submit work as designed
- [ ] Form validation: required fields, format checks (email, phone, etc.)
- [ ] Error handling: validation errors displayed under fields
- [ ] Success feedback: toast/modal confirms action
- [ ] Confirmation: destructive actions (delete) require confirmation

### Accessibility
- [ ] WCAG 2.1 AA: color contrast >4.5:1
- [ ] Keyboard navigation: all interactive elements reachable via Tab
- [ ] Screen reader: semantic HTML, aria-labels on icons
- [ ] Focus management: visible focus ring on interactive elements
- [ ] Form labels: every input has associated label

### Performance
- [ ] Page load <3s on 4G network (target)
- [ ] Images optimized (next/image component)
- [ ] Bundle size impact <50KB
- [ ] No layout shifts (CLS <0.1)

### Testing
- [ ] Unit tests: >80% coverage for component logic
- [ ] Integration tests: happy path + error scenarios
- [ ] E2E tests: user workflows tested end-to-end
- [ ] Visual regression: no unexpected style changes

---
```

### DevOps Story Template

```markdown
# DevOps Story: [Title]

## Acceptance Criteria

### Infrastructure
- [ ] Resource created: [resource name, e.g., "S3 bucket", "RDS instance"]
- [ ] Configuration: all required settings applied (encryption, backups, etc.)
- [ ] Networking: security groups, IAM roles configured
- [ ] Monitoring: CloudWatch/Grafana metrics available
- [ ] Backups: automatic backups enabled (if applicable)

### Deployment
- [ ] GitOps: Infrastructure-as-Code committed to git
- [ ] Terraform: `terraform plan` shows expected changes; `terraform apply` succeeds
- [ ] Multi-environment: dev, stg, prod overlays working
- [ ] Secrets: sensitive data managed via [SecretsManager/ESO/Vault]
- [ ] Rollback: previous version can be restored

### Observability
- [ ] Dashboards: metrics visible in Grafana
- [ ] Alerts: rules defined for critical metrics
- [ ] Logs: centralized in CloudWatch/Loki with correlation IDs
- [ ] Health checks: service health monitored continuously

### Documentation
- [ ] Runbook: operational procedures documented
- [ ] Troubleshooting: common issues and fixes listed
- [ ] On-call: escalation procedures documented

---
```

### QA Story Template

```markdown
# QA Story: [Title]

## Acceptance Criteria

### Test Coverage
- [ ] Unit tests: [X] tests written, >80% coverage
- [ ] Integration tests: happy path + [X] error scenarios
- [ ] E2E tests: [X] user workflows
- [ ] Contract tests: API schema validation passing

### Test Execution
- [ ] All tests pass on main branch
- [ ] No flaky tests (consistent results)
- [ ] Tests run in <[X] minutes
- [ ] CI pipeline gates on test success

### Quality
- [ ] Code coverage: [X]% overall
- [ ] Critical paths: 100% coverage for critical user flows
- [ ] Bug triage: no open critical/high bugs
- [ ] Regression: no regressions from previous sprints

### Documentation
- [ ] Test plan: documented approach, scenarios covered
- [ ] Test data: seeding scripts provided
- [ ] Test reports: coverage, execution time tracked

---
```

---

## WIP Limit Monitoring Sheet

```markdown
# WIP Limit Monitoring — [Sprint Name]

## 🚦 Real-Time Status

| Lane | Current WIP | WIP Limit | Status | Notes |
|------|-----------|-----------|--------|-------|
| 🔧 Backend | 2/3 | 3 | 🟢 Green | 1 slot available |
| 🎨 Frontend | 2/2 | 2 | 🟡 Yellow | AT LIMIT; waiting on 01.03 complete |
| 🚀 DevOps | 2/2 | 2 | 🟡 Yellow | AT LIMIT; no new tasks until 01 complete |
| 🧪 QA | 1/2 | 2 | 🟢 Green | 1 slot available |

**Overall Status:** 🟡 CAUTION — FE and DevOps at WIP limits; monitor closely

---

## Daily Standup Tracking

### Monday (Day 1)
| Lane | What | % Done | Blockers |
|------|------|--------|----------|
| Backend | 01.01 CRUD | 20% | None |
| | 01.03 SLA | 0% | Waiting on DB schema |
| | 01.04 Lifecycle | 0% | Ready |
| Frontend | 01.UI List | 15% | Contract not locked |
| DevOps | Alerts | 40% | Grafana config |
| | S3 | 60% | Access keys pending |
| QA | CRUD Tests | 30% | None |

### Friday (Day 5)
| Lane | What | % Done | Blockers |
|------|------|--------|----------|
| Backend | 01.01 CRUD | 80% | Code review pending |
| | 01.03 SLA | 45% | Flowable timers |
| | 01.04 Lifecycle | 20% | Pending 01.01 merge |
| Frontend | 01.UI List | 75% | Contract locked! |
| DevOps | Alerts | 100% | ✅ Done |
| | S3 | 100% | ✅ Done |
| QA | CRUD Tests | 95% | Pending 01.01 merge |

---

## WIP Violation Log

**If any lane exceeds WIP limit:**

| Date | Lane | Violation | Reason | Action Taken |
|------|------|-----------|--------|--------------|
| 2025-03-15 | FE | 3/2 | New urgent story added | PO approved; added support from BE; removed lower priority story |
| 2025-03-18 | BE | 4/3 | Bug discovery in 01.02 | Fixed priority; deprioritized 01.04 to next sprint |

---

## Sprint Exit Checklist

- [ ] Backend WIP ≤ 3 at sprint end
- [ ] Frontend WIP ≤ 2 at sprint end
- [ ] DevOps WIP ≤ 2 at sprint end
- [ ] QA WIP ≤ 2 at sprint end
- [ ] No violations (or documented exceptions)
- [ ] All "Done" items moved to "Closed"
- [ ] Burndown: velocity within ±10% target

---
```

---

## CSV Import Format

```csv
Sprint,Epic,Story_ID,Title,Lane,Owner,Points,Status,Priority,Acceptance_Criteria,Dependencies,Notes
Sprint 01,Epic-16,16.05,Spring Modulith Config,Backend,@backend-1,13,Ready,High,"ApplicationModules#verify() passes; Event publish/consume working; Boundaries enforced; No cross-module deps",None,CRITICAL PATH
Sprint 01,Epic-16,16.10,PostgreSQL + PgBouncer,DevOps,@devops,13,Ready,High,"PgBouncer running; Schemas created; JDBC pool 20-100; Hikari config; Health check passes",None,Unblocks all DB work
Sprint 01,Epic-16,16.14,CI/CD Workflows Skeleton,DevOps,@devops,8,Ready,High,"GitHub Actions runs; Build passes; Unit tests gated; Manual promotion gates","16.10",CI/CD gates all PRs
Sprint 01,Epic-16,Reserve,Next.js Bootstrap Verification,Frontend,@frontend,3,Ready,Low,"App runs locally; Tailwind loaded; TypeScript strict mode passes",None,Supporting story only
Sprint 02,Epic-16,16.11,DragonflyDB Cache Config,Backend,@backend-2,13,Ready,High,"Cache abstraction layer; Dragonfly pool stable; TTL policies; Metrics tracked",16.05,Prerequisite for caching queries
Sprint 02,Epic-16,16.12,Observability Stack,DevOps,@devops,13,Ready,High,"Victoria Metrics running; Grafana dashboards; OpenTelemetry configured; Alerts defined",16.10,Enables monitoring for all work
Sprint 02,Epic-16,16.13,GitOps Deployment (Flux CD),DevOps,@devops,13,Ready,High,"Flux CD bootstrap; Dev overlay deploys from main; Secrets via SOPS; IaC in git",16.10,Enables auto-deployments
Sprint 02,Epic-16,Support,Telemetry Integration,Frontend,@frontend,5,Ready,Medium,"API client logs; Error boundaries; Trace IDs; No console errors",None,Supporting story
Sprint 03,Epic-00,00.01,Event System + Transactional Outbox,Backend,@backend-1,21,Ready,Critical,"Outbox table created; Poller drains events; p95 <200ms; Idempotency key; Flyway migration",16.05,CRITICAL PATH - blocks all events
Sprint 03,Epic-00,00.03_BE,Link Infrastructure,Backend,@backend-2,13,Ready,High,"Link entity created; Cascades safe; Cross-module queries; Deletion safe; Audit trail",None,Independent domain
Sprint 03,Epic-00,00.03_FE,Link-on-Action UX,Frontend,@frontend,8,Ready,Medium,"Link affordance visible; Link modal; Toast feedback; Bidirectional links visible",00.03_BE,Contract locked by Sprint 02
Sprint 03,Epic-00,OPA,OPA Sidecar Setup (Shadow Mode),DevOps,@devops,13,Ready,High,"OPA sidecar running; Policies injected; Logs captured; Correlation IDs; <50ms latency",None,Infrastructure parallel task
Sprint 03,Epic-00,Contracts,Event Schema Contracts,QA,@qa,8,Ready,High,"AsyncAPI spec; Event examples; Compatibility tests; CI integration",None,Contracts-first approach
Sprint 03,Epic-00,Contracts,Link Endpoint Contracts,QA,@qa,5,Ready,High,"OpenAPI spec; Payload examples; Error docs",None,Contracts-first approach
Sprint 04,Epic-00,00.02,Time Tray MVP,Backend,@backend-1,21,Ready,High,"Mirror worklogs; Time Tray API; Refresh logic; Timestamps accurate; Concurrent safe",00.01,Depends on event system
Sprint 04,Epic-00,00.05_BE,Decision Receipts,Backend,@backend-3,13,Ready,High,"Receipt schema; 100% policy evals logged; Queryable; Immutable (no deletes)",None,Audit trail infrastructure
Sprint 04,Epic-00,00.04,Freshness Badges,Frontend,@frontend,8,Ready,Medium,"Badge renders; Color coding (green/yellow/red); Hover tooltip; Configurable thresholds",None,UX component
Sprint 04,Epic-00,00.05_FE,Policy Studio UI,Frontend,@frontend,13,Ready,Medium,"Shadow mode toggle; Receipt viewer; History search; CSV export",00.05_BE,Audit UI for policies
Sprint 04,Epic-00,OPA,OPA Production Rollout Patterns,DevOps,@devops,13,Ready,High,"Policy deployment via CI/CD; Git versioning; Rollback automation; <30s latency; No traffic loss",OPA,Production readiness
Sprint 04,Epic-00,E2E,Time Tray E2E Flow,QA,@qa,8,Ready,Medium,"Create incident > mirror appears; Modify entry > mirror updates; Concurrent safe",00.02,E2E validation
Sprint 04,Epic-00,E2E,Policy Receipt Validation,QA,@qa,5,Ready,Medium,"Policy eval > receipt created; Receipt immutable; Queryable audit trail",00.05,Audit validation
Sprint 05,Epic-01,01.01,Incident CRUD + Classification,Backend,@backend-1,21,Ready,High,"Incident entity; CRUD API; Validation; Audit log","00.01",Foundational for incidents
Sprint 05,Epic-01,01.03,SLA Timer Tracking (Flowable),Backend,@backend-2,21,Ready,High,"SLA thresholds; Flowable processes; Timer fires; Pre-breach notifications; ±5s accuracy","16.12",Prerequisite for SLA management
Sprint 05,Epic-01,01.04,Incident Lifecycle Workflow,Backend,@backend-3,13,Ready,High,"State machine; Transitions validated; Events published; Role permissions enforced","01.01",Incident state management
Sprint 05,Epic-01,UI,Incident List + Detail Pages,Frontend,@frontend,8,Ready,Medium,"List table; Sorting/filtering; Detail page; Comments stub; Responsive","00.04",UI for incidents
Sprint 05,Epic-01,Alerts,SLA Alerting Setup,DevOps,@devops,13,Ready,High,"Grafana alert rule; Slack routing; Pre-breach 30 min; Includes context; No spam","16.12",Alerting infrastructure
Sprint 05,Epic-01,Storage,Attachment Storage Infrastructure,DevOps,@devops,8,Ready,Medium,"S3 bucket; 50MB limit; Access control; Virus scan ready","16.10",Attachment storage
Sprint 05,Epic-01,WebMvc,Incident CRUD Unit Tests,QA,@qa,8,Ready,High,"Create/Update/Delete tests; State transitions; Cascade behavior",01.01,Unit test coverage
Sprint 05,Epic-01,E2E,Happy Path: Create > Assign,QA,@qa,8,Ready,High,"Create via UI; Search; Assign; SLA visible; List updated",01.01,E2E happy path
```

> **How to Use CSV:**
> 1. Copy table above
> 2. Paste into spreadsheet (Google Sheets, Excel)
> 3. Export as CSV
> 4. Import into Lane via bulk import feature
> 5. Stories auto-create with metadata

---

## How Lane Templates Help

✅ **Reduce planning time:** Pre-structured templates speed up story creation
✅ **Consistency:** All stories follow same format and acceptance criteria standards
✅ **Trackability:** CSV import enables quick backlog setup and bulk updates
✅ **Parallelism visibility:** Swimlane cards clearly show cross-team dependencies
✅ **WIP enforcement:** Monitoring sheet keeps lanes from getting overloaded
✅ **Quality:** Acceptance criteria templates ensure testable, clear requirements

---

> 💡 **Pro Tip**: Copy all relevant sprint sections (e.g., "Sprint 05") and paste into Lane as epic description. Then create individual story cards from the swimlane tables.

