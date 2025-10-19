# 3. CORE FEATURES PHASE EPICS (Months 4-8)

## Epic-03: Self-Service Portal

**Goal:** End-user portal for ticket submission, knowledge search, and status tracking.

**Value Proposition:** Reduce support burden by 20-30% through self-service deflection.

**Story Count:** 5 stories

**Key Capabilities:**
- Portal SSO with user preferences
- Guided ticket submission with dynamic forms
- Knowledge search with previews and ratings
- Service catalog browsing and request submission
- Ticket status tracking for end users

**Dependencies:** Knowledge Management (Epic-05), Service Catalog (Epic-04)

---

## Epic-04: Service Catalog and Request Fulfillment

**Goal:** Service catalog with dynamic forms, approval flows, and fulfillment automation.

**Value Proposition:** Standardize service request processes, automate fulfillment where possible.

**Story Count:** 5 stories

**Key Capabilities:**
- Service entity CRUD with lifecycle management
- Dynamic form schema engine for custom request types
- Approval flow executor per service type
- Fulfillment workflows with audit trail
- Service performance metrics and automation tracking

**Dependencies:** Flowable 7.1.0 workflow engine, OPA 0.68.0 policy engine

---

## Epic-06: Multi-Team Support and Intelligent Routing

**Goal:** Teams, skills, capacity modeling with intelligent routing and escalation.

**Value Proportion:** Reduce misrouted tickets by 70%, improve first-time resolution by 10%.

**Story Count:** 5 stories

**Key Capabilities:**
- Teams, agents, skills, capacity data model
- Scoring engine with policy evaluator for intelligent routing
- Escalation tiers with SLA-based triggers
- Telemetry feedback loop (SLA, CSAT, utilization)
- Business hours, timezone, availability calendars

**Dependencies:** OPA policy engine, User Management (FR-16)

---
