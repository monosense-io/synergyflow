# Epic 4: Workflow Engine - Technical Specification

**Epic:** Epic 4 — Workflow Engine
**Project:** SynergyFlow (Enterprise ITSM & PM Platform)
**Author:** monosense (Architect)
**Date:** 2025-10-06
**Timeline:** Weeks 7–8 (core), continuing enhancements through Week 10
**Team:** 2 BE + 1 FE shared across modules
**Dependencies:** Epics 1–3 complete (foundation, ITSM, PM); Timers and SSE operational
**Project Level:** Level 4 (Platform/Ecosystem)

---

## Overview

This epic implements a simple-yet-powerful workflow engine that delivers approvals, routing automation, state transition rules, and scheduled actions across ITSM and PM. It also introduces the Approval Cockpit with bundles, Policy × Budget × Inventory context, approval intelligence, multi-approver coordination, and simulation. The engine is designed for deterministic behavior, strong auditability, and low-latency decisions, aligning with the Modulith + Companions architecture.

---

## Objectives and Scope

In scope (v1):
- Single-level approvals for service requests with in-app decision UI (FR‑WF‑1,2,3).
- Auto-approve per policy criteria (FR‑WF‑4).
- Routing rules (category/priority/team/workload) (FR‑WF‑5).
- State transition enforcement (blocking invalid paths) (FR‑WF‑6).
- Scheduled actions: auto-close, escalate (uses Timer Service) (FR‑WF‑7).
- Workflow execution audit trail (FR‑WF‑8).
- Admin configuration UI for rules/approvals/timers (FR‑WF‑9,10).
- Approval bundles + P×B×I strip + intelligence + braided timeline + simulation (FR‑WF‑11..15) as MVP features.

Out of scope (v1):
- Multi-stage hierarchical approvals (beyond braided visualization of two decision makers).
- External email/SMS delivery (stub webhooks allowed).

---

## System Architecture Alignment

- Package root: `io.monosense.synergyflow.workflow`.
- Collaborators: `security` (batch auth), `eventing` (outbox), `sla` (timers/calendar), `sse` (realtime), `audit` (log), `itsm` and `pm` via SPI only.
- Read models for inbox and analytics to meet latency budgets (<200–300ms p95).
- Events are authored to outbox; worker fans-out to Streams; app pushes SSE deltas.

---

## Detailed Design

### Module Structure

- API (`workflow/api`)
  - `ApprovalController` — inbox fetch, decisions, bundles.
  - `RulesController` — routing/state rules CRUD.
  - `SimulationController` — approval simulation run.

- SPI (`workflow/spi`)
  - `ApprovalQueryService` — cross-module lookups by target ID.

- Internal (`workflow/internal`)
  - Domain:
    - `ApprovalRequest(id, targetId, type, requesterId, status, createdAt, decidedAt)`
    - `ApprovalDecision(id, requestId, approverId, decision, rationale, decidedAt)`
    - `RoutingRule(id, name, condition, targetTeam, priority, enabled)`
    - `StateRule(id, entityType, from, to, predicate)`
    - `PolicyRule(id, expression)` (for auto-approve)
    - `SimulationModel(inputs, results)`
  - Services:
    - `ApprovalService` (create, decide, bundle)
    - `RoutingService` (evaluate & apply)
    - `StateGuard` (validate transitions)
    - `SchedulerBridge` (auto-close/escalate via `sla` profile)
    - `IntelligenceService` (precedent/confidence scoring)
  - Repositories: JPA for domain + read models
  - Projections (read models): `approval_inbox`, `braided_timeline`, `policy_budget_inventory_card`
  - Events: `ApprovalRequested`, `ApprovalDecided`, `RoutingRuleApplied`, `AutoCloseScheduled`, `EscalationScheduled`

### Data Models (OLTP + Read Models)

OLTP (see V1 schema for `approvals`; extend via Flyway migrations when new approval attributes are required):
- `approvals(id, request_type, target_id, status, created_at, decided_at)`
- `approval_decisions(id, request_id, approver_id, decision, rationale, decided_at)`
- `routing_rules(id, rule_name, condition_type, condition_value, target_team, priority, enabled)`
- `state_rules(id, entity_type, from_status, to_status, predicate)`
- `policy_rules(id, expression)` (simple CEL-like expressions)

Read models:
- `approval_inbox(approver_id, request_id, title, requester, risk_flags[], confidence, created_at, status)`
- `braided_timeline(request_id, actor, action, at)`
- `pbi_card(request_id, policy_match, budget_balance, inventory_on_hand, suggestion)`

### APIs and Interfaces

See OpenAPI stubs: `docs/api/workflow-api.yaml`.

Key endpoints:
- `GET /api/workflow/inbox` — inbox items for current approver.
- `POST /api/workflow/approvals/{requestId}` — decide {APPROVE|REJECT|MORE_INFO} + rationale.
- `POST /api/workflow/approvals/bundles/decide` — bulk approve bundle; server auto-splits outliers.
- `POST /api/workflow/approvals/{requestId}/context` — fetch P×B×I composite context card.
- `GET /api/workflow/approvals/{requestId}/suggestion` — fetch AI-powered approval suggestion.
- `GET /api/workflow/approvals/{requestId}/timeline` — fetch braided multi-approver timeline.
- `POST /api/workflow/rules` — upsert routing/state/policy rules (typed payload).
- `POST /api/workflow/simulate` — run approval simulation with quantity/price tweaks.
- `POST /api/auth/batchCheck` — batch auth (from `security` module).

Cross-module SPIs:
- From ITSM/PM → `RoutingService` for auto-assign; `StateGuard` for transition checks.

### Advanced Workflow Components (FR-WF-11 through FR-WF-15)

#### FR-WF-11: Approval Bundles Architecture

**Component:** `BundleGroupingEngine` (workflow/internal/service)

**Purpose:** Groups pending approval requests by policy archetype and risk profile, enabling single-click bundle approval while auto-breaking out outlier requests.

**Algorithm:**
1. Fetch pending approvals for approver from `approval_inbox` read model
2. Extract policy archetype (e.g., "hardware_request", "software_license", "budget_allocation") from request metadata
3. Calculate risk profile: `risk_score = (cost_usd / 1000) + (urgency_multiplier * 2) + (vendor_risk_flags * 5)`
4. Cluster requests using policy archetype + risk bucket (LOW: 0-10, MEDIUM: 11-30, HIGH: 31+)
5. Auto-split outliers: requests with unique archetype or risk >2σ from bundle mean

**Data Model:**
- OLTP: `approval_bundles(id, approver_id, archetype, risk_bucket, created_at, decided_at)`
- OLTP: `bundle_members(bundle_id, request_id, is_outlier BOOLEAN)`
- Read Model: Derived from `approval_inbox` with aggregation query

**API Contract:**
```
GET /api/workflow/approvals/bundles
Response: [
  {
    bundleId: "bundle-uuid",
    archetype: "hardware_request",
    riskBucket: "MEDIUM",
    requestCount: 12,
    totalCost: 14500,
    requests: [...],
    outliers: [...]
  }
]

POST /api/workflow/approvals/bundles/{bundleId}/decide
Body: { decision: "APPROVE", rationale: "Batch approved per Q4 budget" }
Response: { approvedCount: 12, outlierCount: 2, outlierIds: [...] }
```

**Performance Target:** Bundle grouping <200ms for 100 pending requests; decision processing <300ms per bundle.

**Error Handling:** If outlier detection fails, fall back to individual approval mode with warning banner.

---

#### FR-WF-12: Policy × Budget × Inventory Card Architecture

**Component:** `CompositeContextService` (workflow/internal/service)

**Purpose:** Provides single composite API returning policy match status, budget line availability, and inventory stock levels in one call to eliminate tab-hunting.

**Integration Points:**
- PolicyService (internal): Matches request against policy rules, returns match confidence
- BudgetAdapter (external): Queries budget line balance (stubbed in MVP, real integration Phase 2)
- InventoryAdapter (external): Queries stock-on-hand levels (stubbed in MVP)

**Data Model:**
- Read Model: `pbi_card(request_id, policy_match JSON, budget_balance JSON, inventory_on_hand JSON, suggestion TEXT, fetched_at TIMESTAMP)`
- Cache Strategy: Redis hot cache (TTL 30s) to avoid repeated external calls

**Composite Fetch Algorithm:**
1. Check Redis cache for `pbi_card:{request_id}`
2. If miss, parallel fetch from PolicyService + BudgetAdapter + InventoryAdapter (timeout 200ms each)
3. Assemble composite card with auto-split suggestion if inventory insufficient
4. Write to Redis cache and `pbi_card` read model
5. Return composite JSON

**API Contract:**
```
GET /api/workflow/approvals/{requestId}/context
Response: {
  policyMatch: {
    status: "MATCHED",
    ruleName: "Standard Laptop Request <$2000",
    confidence: 0.95
  },
  budget: {
    lineItem: "Q4 Hardware Budget",
    balance: 45000,
    afterApproval: 43500
  },
  inventory: {
    sku: "LAPTOP-DELL-5530",
    onHand: 7,
    reserved: 2,
    available: 5,
    suggestion: "Approve 7 now, defer 3 to next batch (stock-out in 2 weeks)"
  }
}
```

**Performance Target:** Composite fetch p95 <300ms (parallel execution + cache)

**Fallback Strategy:** If external adapters timeout, return partial card with "Budget/Inventory data unavailable" placeholders.

---

#### FR-WF-13: Approval Intelligence Architecture

**Component:** `ApprovalIntelligenceService` (workflow/internal/service)

**Purpose:** Generates AI-powered approval suggestions with confidence scores based on precedent analysis and risk signals.

**ML Pipeline (MVP Approach):**
1. **Precedent Matching:** Vector similarity search using sentence-transformers embeddings
   - Encode request text (title + description) to 384-dim vector
   - Query historical approved/rejected requests via cosine similarity
   - Top-5 precedents with similarity >0.7 used for confidence scoring
2. **Confidence Calculation:** `confidence = (approved_count / total_precedents) * similarity_weight`
3. **Risk Flag Extraction:** Query vendor recall feeds, security advisories (external APIs)
4. **Fallback:** If ML service unavailable, use rule-based heuristics (cost thresholds, tenure checks)

**Data Model:**
- OLTP: `approval_precedents(id, request_vector VECTOR(384), decision, rationale, created_at)` (pgvector extension)
- Read Model: `approval_suggestions(request_id, confidence FLOAT, precedents JSON, risk_flags JSON, rationale TEXT)`

**API Contract:**
```
GET /api/workflow/approvals/{requestId}/suggestion
Response: {
  suggestion: "APPROVE",
  confidence: 0.93,
  rationale: "93% of similar requests approved (15 precedents)",
  precedents: [
    { id: "req-123", similarity: 0.89, decision: "APPROVE", rationale: "Within budget, standard hardware" },
    ...
  ],
  riskFlags: [
    { type: "VENDOR_RECALL", severity: "LOW", message: "Minor firmware issue patched" }
  ],
  signals: [
    "Cost within policy threshold (<$2000)",
    "Requester tenure >6 months",
    "No security advisories for SKU"
  ]
}
```

**Performance Target:** Suggestion generation p95 <500ms (vector search + risk API calls)

**Error Handling:** If ML service unavailable, return rule-based suggestion with `confidence: 0.5` and note: "AI assistant unavailable, using policy rules".

---

#### FR-WF-14: Multi-Approver Coordination Architecture

**Component:** `BraidedTimelineService` (workflow/internal/service)

**Purpose:** Displays coordinated approval timeline for requests requiring multiple approvers (Finance + IT), identifies long-pole approver, and provides nudge actions.

**Timeline Construction Algorithm:**
1. Fetch approval request with `required_approvers` list (Finance, IT)
2. Query `approval_decisions` for completed actions
3. Identify pending approvers and calculate SLA countdown (48h escalation timer)
4. Determine long-pole: approver with longest elapsed time or highest SLA risk
5. Build braided timeline JSON with parallel tracks

**Data Model:**
- OLTP: `approval_coordinators(request_id, approver_role, status, notified_at, decided_at)`
- Read Model: `braided_timeline(request_id, tracks JSON, long_pole_approver TEXT, sla_countdown INTERVAL)`

**API Contract:**
```
GET /api/workflow/approvals/{requestId}/timeline
Response: {
  tracks: [
    {
      role: "Finance",
      approver: "sarah@example.com",
      status: "APPROVED",
      decidedAt: "2025-10-05T14:30:00Z",
      duration: "2h 15m"
    },
    {
      role: "IT",
      approver: "john@example.com",
      status: "PENDING",
      elapsedTime: "18h 45m",
      slaRemaining: "5h 15m"
    }
  ],
  longPoleApprover: "IT (john@example.com)",
  slaCountdown: "5h 15m remaining",
  nudgeAction: {
    available: true,
    endpoint: "POST /api/workflow/approvals/{requestId}/nudge",
    context: "Attach: Budget approved, IT decision required for deployment"
  }
}
```

**Performance Target:** Timeline fetch p95 <200ms

**Nudge Action:** Sends in-app notification + email with approval context and SLA urgency to long-pole approver.

---

#### FR-WF-15: Approval Simulation Architecture

**Component:** `SimulationEngine` (workflow/internal/service)

**Purpose:** Enables managers to simulate approval decisions before committing, showing budget impact, inventory depletion, fulfillment ETA, and policy breach probability.

**Simulation Algorithm:**
1. Accept input: `requestId`, `quantity` (tweak), `priceOverride` (optional)
2. Clone request context (policy, budget, inventory) into in-memory simulation model
3. Calculate deltas:
   - **Budget Impact:** `delta = quantity * unit_price`
   - **Inventory Depletion Curve:** Project stock-out date using `(on_hand - quantity) / avg_daily_burn_rate`
   - **Fulfillment ETA:** Query warehouse queue depth + fulfillment SLA (e.g., 3-5 business days)
   - **Breach Probability:** Check if delta violates budget cap or inventory safety stock (P=1.0 if breach, P=0.0 if safe)
4. Return simulation results (no persistence)

**Data Model:**
- No OLTP (ephemeral simulation)
- Input: `SimulationRequest(requestId, quantity, priceOverride)`
- Output: `SimulationResult(budgetDelta, budgetBalanceAfter, inventoryDepletionDate, fulfillmentEta, breachProbability, warnings[])`

**API Contract:**
```
POST /api/workflow/simulate
Body: {
  requestId: "req-uuid",
  quantity: 10,
  priceOverride: null
}
Response: {
  budgetDelta: -15000,
  budgetBalanceAfter: 30000,
  inventoryDepletion: {
    stockOutDate: "2025-10-20",
    daysUntilStockOut: 14,
    safetyStockBreached: false
  },
  fulfillmentEta: {
    queueDepth: 12,
    estimatedDays: 4,
    deliveryDate: "2025-10-10"
  },
  breachProbability: 0.0,
  warnings: []
}

// With quantity that exceeds budget
Body: { requestId: "req-uuid", quantity: 50 }
Response: {
  ...
  breachProbability: 1.0,
  warnings: [
    "Budget cap exceeded by $20,000",
    "Safety stock breached (5 units remaining, recommended minimum 10)"
  ]
}
```

**Performance Target:** Simulation p95 <500ms for 20 what-if scenarios (parallel execution for batch simulations)

**Live Quantity Tweaking:** Frontend slider updates quantity, debounces 300ms, then re-submits simulation for live feedback.

---

### Component Integration Flow (Advanced Workflow)

```
Approval Request Lifecycle with Advanced Features:

1. Request Created
   ↓
2. BundleGroupingEngine clusters into bundles (if >10 pending)
   ↓
3. Manager opens Approval Cockpit
   ↓
4. CompositeContextService fetches P×B×I card (parallel: policy + budget + inventory)
   ↓
5. ApprovalIntelligenceService generates suggestion (ML precedent matching + risk flags)
   ↓
6. BraidedTimelineService shows multi-approver coordination (Finance + IT tracks)
   ↓
7. Manager runs SimulationEngine (tweaks quantity, sees budget/inventory impact)
   ↓
8. Manager decides:
   - Single approval → POST /api/workflow/approvals/{id}
   - Bundle approval → POST /api/workflow/approvals/bundles/{bundleId}/decide
   ↓
9. ApprovalDecided event → outbox → SSE → UI update <2s
   ↓
10. Audit trail written, target state updated, timers adjusted
```

### Workflows and Sequencing

1) Submission → `ApprovalRequested` (outbox). Inbox read model inserts row; SSE pushes to approver.
2) Approver decision → `ApprovalDecided` → state update on target, audit entry, schedule follow-ons (escalation cancel, auto-close timer).
3) Routing rule change → `RoutingRuleApplied` logged; subsequent creates use updated evaluation.
4) Simulation → compute P×B×I context with stubbed budget/inventory adapters; return outcome deltas.

---

## Non-Functional Requirements

Performance
- Inbox fetch p95 <200ms; decision post p95 <200ms; simulation p95 <500ms (stub adapters).

Security
- OIDC JWT; server-side scope filters; all decisions audited; decisions require explicit actor ID.

Reliability/Availability
- Idempotent decisions (protect against double-submit); timers are durable (missed=0, drift<1s per performance model).

Observability
- Traces: decision, routing evaluate, simulation; Metrics: p95/99 decision latency, bundle split rate, timer drift; Logs: policy matches, rule hits, audit entries.

---

## Dependencies and Integrations
- `security` (batch auth), `sla` (timers/calendar), `eventing` (outbox/streams), `sse` (push), `audit` (writes), adapters for budget/inventory (stubbed).

---

## Acceptance Criteria (Authoritative)
1. Approver sees pending requests in inbox with context (FR‑WF‑1,2).
2. Decision actions apply state to target, write audit, and reflect in UI within 2s (FR‑WF‑2,3,8,9).
3. Auto-approve policy applies when criteria match; otherwise routes to approver (FR‑WF‑4).
4. Routing rules auto-assign on create; changes take effect for new items (FR‑WF‑5).
5. Invalid state transitions are blocked with clear errors (FR‑WF‑6).
6. Auto-close after 7 days; escalate pending approvals after 48h (calendar-aware) (FR‑WF‑7).
7. Approval bundles allow single-click approval while auto-splitting outliers (FR‑WF‑11).
8. P×B×I card displayed on request with suggestion when stock insufficient (FR‑WF‑12).
9. Approval intelligence shows confidence score and precedent rationale with “Why?” signals (FR‑WF‑13).
10. Braided timeline shows Finance + IT progress; nudge action available with countdown (FR‑WF‑14).
11. Simulation returns budget delta, stock depletion curve, ETA, and breach probability as quantity changes (FR‑WF‑15).

---

## Traceability Mapping (Excerpt)

| FR | Component/API | Read Model | Test Idea |
|---|---|---|---|
| WF‑1/2 | `ApprovalController.inbox/decide` | `approval_inbox` | Decision updates target, inbox clears, SSE <2s |
| WF‑5 | `RoutingService.evaluate` | n/a | Create ticket with category → auto-assigned team |
| WF‑7 | `SchedulerBridge` + `sla` | n/a | Resolve ticket → auto-close timer created, fires after 7d |
| WF‑11 | `ApprovalService.decideBundle` | `approval_inbox` | Mixed bundle → inliers approved, outliers remain |

---

## Risks, Assumptions, Open Questions
- Risk: Policy engine complexity → start with simple expressions; plan for CEL/SpEL hardening later.
- Risk: Budget/inventory integration variability → provide adapter layer; stub for MVP.
- Assumption: Single-level approvals suffice for Phase 1; braided view coordinates multiple roles.
- Question: Who owns P×B×I data sources? Define owner and SLA before GA.

---

## Test Strategy Summary
- Unit: policy evaluation, routing, state guard, bundle splitting.
- Integration: decision → audit/log → outbox → SSE; timer schedule and fire; idempotent decisions.
- Contract: OpenAPI for inbox/decide/rules/simulate; batch auth contract.
- E2E: Approver inbox → decide; bundle approve with outliers; auto-close/escalate timelines.
- Performance: decision p95 <200ms; simulation p95 <500ms; event E2E ≤2s.
