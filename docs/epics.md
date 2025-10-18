# synergyflow - Epic Breakdown

**Author:** Eko Purwanto
**Date:** 2025-10-18
**Project Level:** Level 4 (Platform/Ecosystem)
**Target Scale:** 250-1,000 users

---

### Epic 00 — Trust + UX Foundation Pack

Expanded Goal: Provide a searchable, governed knowledge base with versioning and approvals that reduces incident volume and speeds resolution through reusable, high‑quality articles.

Value Proposition: Self‑service deflection and faster MTTR by enabling agents and end users to find and trust vetted knowledge.

Stories

**Story [05.1]: Knowledge article CRUD with versioning**

As a knowledge manager,
I want to create and revise articles with semantic versioning,
So that updates are auditable and reversible.

Acceptance Criteria:
1. Article CRUD with fields: title, body (Markdown), tags, owner, status, version (v1.0, v1.1, v2.0).
2. Version history retained with diffs; restore to prior version.
3. API + UI with role‑based permissions.

Prerequisites: [00.1].

**Story [05.2]: Approval workflow**

As an editor,
I want a Draft → Review → Approved → Published workflow,
So that content quality and governance are ensured.

Acceptance Criteria:
1. State machine with approver assignment; timestamps captured on transitions.
2. Notifications on submit/review/approve/reject.
3. Only Approved content can be Published; audit trail maintained.

Prerequisites: [05.1].

**Story [05.3]: Full‑text search with relevance ranking**

As a support agent,
I want fast, relevant knowledge search,
So that I can resolve incidents quickly.

Acceptance Criteria:
1. Full‑text search across title/body/tags with relevance scoring.
2. Highlighted matches in results; filters by tag/status/owner.
3. P95 query latency target ≤200 ms on MVP dataset.

Prerequisites: [05.1].

**Story [05.4]: Tagging and related articles**

As a reader,
I want tags and related links,
So that I can navigate adjacent knowledge.

Acceptance Criteria:
1. Tag CRUD with article associations; tag facet filters in UI.
2. Related articles suggested by tag/embedding similarity.
3. Article detail shows related list; API exposes related endpoint.

Prerequisites: [05.1].

**Story [05.5]: Expiry notification scheduler**

As a content owner,
I want reminders before articles go stale,
So that knowledge remains accurate.

Acceptance Criteria:
1. Expiry date per article; scheduler sends notifications 30 days prior.
2. Owner can snooze or update article; state changes recorded.
3. Reporting of soon‑to‑expire and expired articles.

Prerequisites: [05.1].

---

### Epic 01 — Incident and Problem Management

Expanded Goal: Establish a production‑ready platform foundation with high availability data services, observability, and GitOps delivery so teams can deploy and operate reliably from day one.

Value Proposition: Fast, repeatable deployments with HA databases, consistent configuration, and actionable telemetry — reducing operational toil and risk.

Stories

**Story [16.1]: Backend monolith bootstrap (Status: Completed)**

As a platform engineer,
I want a bootstrapped backend monolith project,
So that development can start on a coherent foundation.

Acceptance Criteria:
1. Spring Boot project scaffolded with module boundaries.
2. Build/test pipelines runnable locally; container build scripts exist.
3. Baseline CI job runs unit tests on push.

Prerequisites: —

**Story [16.2]: Frontend app bootstrap Next.js (Status: Completed)**

As a frontend engineer,
I want a Next.js app scaffold,
So that UI work can proceed with consistent tooling.

Acceptance Criteria:
1. Next.js app created with TypeScript and linting.
2. Auth and API client placeholders wired to gateway.
3. Containerization set up for dev/stg/prod.

Prerequisites: —

**Story [16.3]: Backend OAuth2 Resource Server JWT (Status: Completed)**

As a security engineer,
I want JWT validation in the backend,
So that APIs are protected consistently.

Acceptance Criteria:
1. Spring Security resource server with JWT validation configured.
2. Audience/issuer validation; scopes for core routes.
3. Integration test proving 401/403 behavior.

Prerequisites: [16.1].

**Story [16.4]: Gateway JWT validation (Envoy) (Status: Completed)**

As a platform engineer,
I want JWT validation at the gateway,
So that perimeter enforcement blocks unauthenticated requests.

Acceptance Criteria:
1. Envoy configured for JWT validation against issuer.
2. Pass‑through of identity claims to backend.
3. Negative tests confirm rejection of invalid tokens.

Prerequisites: —

**Story [16.5]: Spring Modulith configuration**

As an architect,
I want Spring Modulith and event publication configured,
So that modules communicate reliably with durable delivery.

Acceptance Criteria:
1. Module boundaries defined; ApplicationEvents use Java records.
2. `event_publication` table created with indexes; retry workflow in place.
3. Outbox registry and consumer idempotency verified with tests.

Prerequisites: [16.1].

**Story [16.6]: PostgreSQL HA (CloudNative‑PG)**

As a DBA/SRE,
I want a 3‑instance HA PostgreSQL cluster with backups,
So that data is durable and recoverable.

Acceptance Criteria:
1. CloudNative‑PG manifests deployed (3 replicas) with automated backups (30‑day retention to S3).
2. Readiness/liveness probes and connection secrets configured.
3. Failover tested; application reconnect verified.

Prerequisites: Kubernetes cluster, storage class.

**Story [16.7]: DragonflyDB cache configuration**

As a platform engineer,
I want a shared DragonflyDB cache configured,
So that services can share low‑latency caching.

Acceptance Criteria:
1. DragonflyDB service deployed and reachable at `dragonfly.dragonfly-system.svc`.
2. App connection properties and health checks configured.
3. Cache usage metrics emitted.

Prerequisites: Kubernetes cluster.

**Story [16.8]: Observability stack (VictoriaMetrics + Grafana + OpenTelemetry)**

As an SRE,
I want metrics, logs, and tracing wired up,
So that we can monitor SLOs and debug issues quickly.

Acceptance Criteria:
1. Dashboards show API latency, event lag, and error rate.
2. OpenTelemetry tracing propagates correlation IDs end‑to‑end.
3. Alerts configured for key SLO breaches.

Prerequisites: [16.5].

**Story [16.9]: GitOps deployment (Flux CD + Kustomize)**

As a platform engineer,
I want Git‑driven deployments across dev/stg/prod,
So that changes are auditable and rollbacks are simple.

Acceptance Criteria:
1. Flux CD manages apps with Kustomize overlays for dev/stg/prod.
2. Image updates automated; promotion flows documented.
3. Rollback procedure validated in staging.

Prerequisites: Cluster access; container registry configured (Harbor).

---

### Epic 02 — Change and Release Management

Expanded Goal: Provide an end‑user portal for ticket submission, knowledge search, and status tracking that reduces support load through intuitive self‑service and guided flows.

Value Proposition: Deflect tickets and accelerate resolution by enabling users to help themselves with smart forms, searchable knowledge, and transparent status.

Stories

**Story [03.1]: Portal SSO with user preferences**

As an end user,
I want to sign in via the organization’s SSO and manage notification/preferences,
So that I can access the portal securely with a personalized experience.

Acceptance Criteria:
1. SSO via existing identity provider; session management and logout.
2. Preferences for notifications and default views stored per user.
3. Accessibility baseline: keyboard‑first navigation; WCAG 2.1 AA contrast.

Prerequisites: [16.4], [12.x].

**Story [03.2]: Guided ticket submission with dynamic forms**

As an end user,
I want guided forms that adapt to my issue type,
So that I submit accurate requests without back‑and‑forth.

Acceptance Criteria:
1. Dynamic schema‑driven forms with validation and helpful hints.
2. Attachment support; preview before submit; clear confirmation.
3. Submitted tickets create Incidents or Service Requests with links back to the portal.

Prerequisites: [01.1], [04.x].

**Story [03.3]: Knowledge search with previews and ratings**

As an end user,
I want to search and preview knowledge articles and rate usefulness,
So that I can self‑resolve common issues.

Acceptance Criteria:
1. Full‑text search with highlighted matches; filters by tag and freshness.
2. Inline preview card; open article in portal; rating widget persisted.
3. Telemetry on search → click → resolve funnel.

Prerequisites: [05.1], [05.3].

**Story [03.4]: Service catalog browsing and request submission**

As an end user,
I want to browse a service catalog and request standard services,
So that I can quickly access common offerings.

Acceptance Criteria:
1. Catalog list and detail; per‑service request form and SLAs.
2. Approval routing hooks ready (manual initially); request tracking visible.
3. Links and status synced to backend records.

Prerequisites: [04.x].

**Story [03.5]: Ticket status tracking for end users**

As an end user,
I want to see my ticket statuses and history,
So that I’m informed without contacting support.

Acceptance Criteria:
1. My Tickets list with filters; detail view shows timeline and comments.
2. Live updates on status/comments via event stream; freshness badges.
3. Email/link deep‑links open the exact ticket in portal.

Prerequisites: [00.1], [01.4].

---

### Epic 03 — Self‑Service Portal

Expanded Goal: Provide a configurable service catalog with dynamic request forms, approval execution, and fulfillment workflows so standard services are delivered consistently with auditability.

Value Proposition: Standardize and accelerate common requests with guardrails and visibility, reducing manual coordination and errors.

Stories

**Story [04.1]: Service entity CRUD with lifecycle**

As a service owner,
I want to define services with lifecycle states,
So that offerings are curated and governable.

Acceptance Criteria:
1. Service CRUD with fields: name, description, owner, SLA targets, status (Draft, Active, Deprecated).
2. Listing, filters, and detail UI; role‑based permissions.
3. Audit trail of lifecycle changes.

Prerequisites: [00.1].

**Story [04.2]: Dynamic form schema engine**

As a service owner,
I want to attach schema‑driven forms to services,
So that requesters provide the right data for fulfillment.

Acceptance Criteria:
1. JSON‑schema (or equivalent) form definitions with validation and conditional fields.
2. Reusable components (text, select, file, date, multi‑select); preview in admin UI.
3. Versioning of form schema with migration guidance.

Prerequisites: [04.1].

**Story [04.3]: Approval flow executor (manual initial)**

As a release/approvals manager,
I want approvals to be routed and recorded per service type,
So that governance is applied before fulfillment.

Acceptance Criteria:
1. Configurable approver list/tiers per service; request → approved/rejected states.
2. Notifications for pending decisions; timestamps captured.
3. Hooks designed for policy‑based auto‑approval in Phase 2.

Prerequisites: [04.1], [02.2].

**Story [04.4]: Fulfillment workflows with audit trail**

As a fulfiller,
I want guided fulfillment steps with auditability,
So that delivery is consistent and traceable.

Acceptance Criteria:
1. Flowable workflow executes fulfillment steps; task assignments and timers supported.
2. All actions logged with operator, time, and outcome; artifacts attachable.
3. Customer visible status updates synced to portal.

Prerequisites: [04.1].

**Story [04.5]: Service performance metrics and automation tracking**

As a product owner,
I want metrics on request throughput, SLA adherence, and automation rate,
So that I can improve services.

Acceptance Criteria:
1. Dashboards for request volumes, lead time, SLA breaches.
2. Automation ratio tracked (manual vs policy‑approved vs auto‑fulfilled).
3. Export endpoints for reporting.

Prerequisites: [16.8].

---

### Epic 04 — Service Catalog and Request Fulfillment

Expanded Goal: Model teams, skills, and capacity; route tickets intelligently using a scoring engine with policy evaluation and clear escalation paths, improving assignment quality and response times.

Value Proposition: Reduce misrouted tickets by ~70% and improve first‑time resolution by ~10% through skill‑aware routing and SLA‑driven escalations.

Stories

**Story [06.1]: Teams, agents, skills, and capacity model**

As an admin,
I want to define teams, agents, skills, and capacity,
So that routing decisions have accurate inputs.

Acceptance Criteria:
1. Data model for Teams, Agents, Skills, SkillLevels, and Capacity (per agent/team).
2. CRUD APIs and UI with RBAC; audit changes with who/when.
3. Capacity fields include available hours and max concurrent workload.

Prerequisites: [16.1], [12.x].

**Story [06.2]: Scoring engine with policy evaluator for routing**

As a dispatcher,
I want the system to recommend the best assignee based on skills, load, and priority,
So that tickets land with the right agent the first time.

Acceptance Criteria:
1. Routing score = f(skill match, current load, priority/severity, recent performance).
2. OPA policy hook allows constraints/overrides (e.g., exclude on vacation, after‑hours rules).
3. Recommendation API returns top N candidates with reason codes; manual override preserved.

Prerequisites: [06.1], [00.1], [12.x].

**Story [06.3]: Escalation tiers with SLA‑based triggers**

As a service manager,
I want escalation tiers that trigger on SLA risk/breach,
So that critical work gets prompt attention.

Acceptance Criteria:
1. Configurable escalation tiers (e.g., Tier 1 team → Tier 2 specialist → Manager).
2. Triggers on predicted breach and actual breach; notifications and reassignment logged.
3. Dashboards show escalations over time; policies configurable per queue.

Prerequisites: [01.3], [06.2].

**Story [06.4]: Telemetry feedback loop (SLA, CSAT, utilization)**

As a product owner,
I want routing models to learn from outcomes,
So that recommendations improve over time.

Acceptance Criteria:
1. Collect features: assignment outcome, SLA hit/miss, reassignments, CSAT, handle time.
2. Periodic scoring weights update or rules refinement; changes tracked.
3. Reports show routing accuracy and impact on SLA/CSAT.

Prerequisites: [16.8], [06.2].

**Story [06.5]: Business hours, timezone, and availability calendars**

As a scheduler,
I want business hours and availability considered in routing,
So that off‑hours and holidays are handled correctly.

Acceptance Criteria:
1. Business hours per team/timezone; holiday calendars; agent PTO/OOO integration.
2. Routing excludes unavailable agents; after‑hours policies apply automatically.
3. UI indicators for availability; APIs expose calendars.

Prerequisites: [06.1].

---

### Epic 05 — Knowledge Management

Expanded Goal: Track assets end‑to‑end with ownership, lifecycle states, and license compliance, providing searchable inventory and field‑friendly updates.

Value Proposition: Prevent license violations and reduce incident load by maintaining accurate, actionable asset data.

Stories

**Story [07.1]: Asset model and lifecycle**

As a configuration manager,
I want an asset data model with lifecycle states,
So that inventory is consistent and auditable.

Acceptance Criteria:
1. Entities: Asset, Type, Owner, Location, Status (Draft, Active, In Repair, Retired), Serial, Tags.
2. CRUD APIs and UI; validation for unique serial/asset tag.
3. Lifecycle transitions audited with timestamps and actor.

Prerequisites: [16.1].

**Story [07.2]: Discovery pipelines with normalization**

As an asset engineer,
I want ingestion pipelines to import device data from sources,
So that inventory stays current.

Acceptance Criteria:
1. Ingest CSV/JSON feeds and agent exports; map fields to canonical schema.
2. Deduplicate by serial/MAC; normalization rules for model/vendor names.
3. Import jobs observable with success/failure metrics.

Prerequisites: [07.1].

**Story [07.3]: License monitoring and compliance alerts**

As a compliance officer,
I want license usage tracked against entitlements,
So that we detect over‑utilization and expired licenses.

Acceptance Criteria:
1. Entitlement model (product, seats, expiry) and usage counters by asset/user.
2. Alerts for approaching/exceeded limits and upcoming expiry (30‑day notice).
3. Reports exportable (CSV) for audits.

Prerequisites: [07.1].

**Story [07.4]: Mobile barcode/QR scanning for updates**

As a field technician,
I want to scan a barcode/QR to view and update asset info,
So that on‑site changes are fast and accurate.

Acceptance Criteria:
1. Mobile‑friendly page with camera scanning; resolves asset by code.
2. Update owner/location/status with offline‑tolerant queue.
3. Audit entry created for every field change with geotag (if permitted).

Prerequisites: [07.1].

---

### Epic 06 — Multi‑Team Support and Intelligent Routing

Expanded Goal: Build a Configuration Management Database (CMDB) with a first‑class relationship graph and impact analysis integrated into Change Management, so teams can assess blast radius and dependencies before making changes.

Value Proposition: Fewer incidents caused by changes and faster root‑cause analysis through accurate CI relationships, snapshots, and impact evaluation.

Stories

**Story [08.1]: CI model with types and relationships**

As a configuration manager,
I want CI types, items, and relationship types modeled,
So that we can represent systems and their dependencies consistently.

Acceptance Criteria:
1. Entities: CIType, ConfigurationItem, Relationship (typed: depends_on, hosts, connects_to) with direction.
2. CRUD APIs and UI for CI/relationship management; uniqueness and referential integrity enforced.
3. Audit trail on create/update/delete with user and timestamp.

Prerequisites: [16.1], [07.1].

**Story [08.2]: Relationship graph traversal APIs**

As a developer/analyst,
I want APIs to traverse CI relationships,
So that upstream/downstream impacts can be explored programmatically and in UI.

Acceptance Criteria:
1. Endpoints for upstream/downstream traversal with depth limit and relationship‑type filters.
2. P95 traversal latency ≤200 ms on MVP dataset; pagination for large graphs.
3. Security: results scoped by user permissions on CIs.

Prerequisites: [08.1].

**Story [08.3]: Baseline snapshot and diff management**

As an SRE,
I want to capture and compare CMDB snapshots,
So that I can see what changed between two points in time.

Acceptance Criteria:
1. Snapshot create/list/restore with metadata (who, when, reason).
2. Diff view/API highlights added/removed/changed CIs and relationships.
3. Restore is permission‑gated and audited; supports dry‑run preview.

Prerequisites: [08.1].

**Story [08.4]: Impact assessment for Change Management**

As a change approver,
I want automatic impact analysis for a proposed change,
So that I understand affected CIs and risk before approval.

Acceptance Criteria:
1. Given a change (service/module/CIs), compute impacted set via graph traversal; show blast radius.
2. Risk score calculated from CI criticality and path length; results attached to change record.
3. UI panel in change view shows impacted CIs with links; pre‑approval check blocks if risk > threshold (configurable).

Prerequisites: [02.1], [02.3], [08.2].

**Story [08.5]: Data quality governance and validation**

As a CMDB steward,
I want automated data quality checks and remediation,
So that the CMDB remains trustworthy.

Acceptance Criteria:
1. Validators for required fields, orphaned relationships, and type constraints; nightly job runs checks.
2. Data quality score per CI and overall; dashboard and export.
3. Remediation queue with assign/resolve workflow; changes audited.

Prerequisites: [08.1].

---

### Epic 07 — IT Asset Management (ITAM)

Expanded Goal: Provide customizable dashboards and a guardrailed report builder so teams get real‑time visibility into operational metrics without compromising performance or security.

Value Proposition: Data‑driven decisions via live tiles, report scheduling, and pre‑defined templates aligned with governance.

Stories

**Story [09.1]: Dashboard model with widget framework**

As a team lead,
I want configurable dashboards with drag‑and‑drop widgets,
So that I can monitor what matters to my team.

Acceptance Criteria:
1. Widgets for key metrics (tickets by status, SLA breaches, event lag, policy decisions).
2. Layout persistence per user/team; permissions enforced.
3. Drag‑and‑drop with responsive grid; export snapshot as image/pdf.

Prerequisites: [16.8].

**Story [09.2]: Live tiles with event stream updates**

As a user,
I want tiles that update in near real‑time,
So that I see current status without refreshing.

Acceptance Criteria:
1. Event stream subscription updates tile data; freshness badges displayed.
2. Back‑pressure/aggregation guards prevent overload.
3. p95 update latency ≤2s from event to tile update.

Prerequisites: [00.1], [16.8].

**Story [09.3]: Report builder with templates and guardrails**

As an analyst,
I want a guided report builder with templates,
So that I can answer questions without breaking systems.

Acceptance Criteria:
1. Pre‑defined templates (incidents by priority, SLA trends, change success rate).
2. Parameterized queries with limits; RBAC‑scoped results.
3. Exports (CSV/PDF) with lineage metadata.

Prerequisites: [16.8].

**Story [09.4]: Report scheduler with email delivery**

As a manager,
I want scheduled report delivery,
So that stakeholders receive regular updates automatically.

Acceptance Criteria:
1. Schedules (cron presets); email delivery with attachments/links.
2. Audit log of deliveries; failure alerts.
3. Unsubscribe and preference controls per user.

Prerequisites: [11.x].

---

### Epic 08 — CMDB and Impact Assessment

Expanded Goal: Deliver governed analytics interfaces with secure dataset contracts, first‑class BI connections, scheduled refresh, and seed ML pipelines to enable predictive insights (incident spikes, capacity planning).

Value Proposition: Unlock data for decision‑makers without risking prod systems; accelerate forecasting and optimization.

Stories

**Story [10.1]: Dataset export services with row‑level security**

As a data consumer,
I want stable dataset contracts with row‑level security,
So that I can analyze operational data safely.

Acceptance Criteria:
1. Versioned dataset definitions (Incidents, Changes, Worklogs) with documented schemas.
2. RLS enforced by user/org scope; tokenized access via service accounts.
3. Export endpoints (SQL views and API) with pagination and rate limits.

Prerequisites: [09.1], [12.x].

**Story [10.2]: BI connectivity (Tableau, Power BI, Looker)**

As a BI analyst,
I want native connectivity and docs for major BI tools,
So that I can build dashboards quickly.

Acceptance Criteria:
1. Connection guides and example workbooks/datasets for Tableau/Power BI/Looker.
2. OAuth/service account patterns documented; secrets handled via ESO.
3. Smoke tests validate connectivity and row‑level scoping.

Prerequisites: [10.1].

**Story [10.3]: Refresh scheduler with lineage tracker**

As a data engineer,
I want scheduled dataset refresh with lineage tracking,
So that stakeholders get timely, traceable data.

Acceptance Criteria:
1. Cron presets per dataset; success/failure logs with metrics.
2. Lineage records (source → dataset → report) with timestamps and versions.
3. Alerts on failed refresh; retry/backoff strategy.

Prerequisites: [10.1], [11.x].

**Story [10.4]: ML pipeline templates for prediction**

As a data scientist,
I want starter pipelines for incident‑spike and capacity‑planning predictions,
So that we can prototype forecasting rapidly.

Acceptance Criteria:
1. Data extraction → feature engineering notebooks; baseline models with evaluation metrics.
2. Reproducible runs and artifact storage; clear handoff to productionization.
3. Ethical/use‑policy note; no automated actions without human gate.

Prerequisites: [10.1], [09.3].

---

### Epic 09 — Dashboards and Reports

Expanded Goal: Provide a multi‑channel notification platform with per‑user preferences, deduplication, grouping, and delivery metrics to ensure timely, useful alerts without fatigue.

Value Proposition: Right signal, right channel, right time — reduce noise while increasing awareness.

Stories

**Story [11.1]: Template engine with localization**

As a content designer,
I want templated notifications with localization support,
So that messages are consistent and multilingual.

Acceptance Criteria:
1. Template variables and conditionals; preview and test send.
2. Locale resolution per user; fallbacks and pluralization supported.
3. Versioning of templates with audit trail.

Prerequisites: —

**Story [11.2]: User preference subscriptions**

As a user,
I want to control which notifications I receive on which channels,
So that I only see what’s relevant.

Acceptance Criteria:
1. Subscriptions model per event type and channel (email, in‑app, push).
2. UI to manage preferences; defaults by role/tenant.
3. Enforcement at send time; respect DND windows.

Prerequisites: [11.1].

**Story [11.3]: Channel adapters (email, push, in‑app)**

As a platform,
I want pluggable adapters for delivery channels,
So that we can route messages reliably.

Acceptance Criteria:
1. Email (SMTP), Push (FCM/APNs), In‑app center with unread counts.
2. Retries and dead‑letter queues for failures; metrics exported.
3. Provider credentials stored via ESO.

Prerequisites: [11.2].

**Story [11.4]: Deduplication, grouping, digest mode**

As a user,
I want duplicate alerts reduced and grouped digests,
So that I’m not overwhelmed.

Acceptance Criteria:
1. Dedup rules per event type; suppression windows.
2. Grouping logic by entity; hourly/daily digests.
3. Analytics on suppression/digest outcomes.

Prerequisites: [11.3].

**Story [11.5]: Delivery metrics and audit reporting**

As a manager,
I want delivery metrics and audit trails,
So that I can ensure notifications are effective and compliant.

Acceptance Criteria:
1. Metrics: sent, delivered, failed, opened (if applicable).
2. Audit log per notification with recipient, channel, status.
3. Reports exportable; retention policy documented.

Prerequisites: [11.3].

---

### Epic 10 — Analytics and BI Integration

Expanded Goal: Centralize authorization, SSO, audit logging, and secrets management to reach enterprise‑grade security baselines and compliance readiness (SOC 2, ISO 27001).

Value Proposition: Consistent, explainable security decisions with evidence trails; easier audits and safer operations.

Stories

**Story [12.1]: AuthZ service with RBAC/ABAC (OPA policies)**

As a security engineer,
I want a dedicated authorization service powered by OPA,
So that access decisions are consistent and explainable.

Acceptance Criteria:
1. Policy bundles versioned; decisions include receipt with inputs/policy/version/outcome.
2. RBAC roles + ABAC attributes supported; deny‑by‑default stance.
3. Latency p95 <100 ms for policy evaluation.

Prerequisites: [16.4], [00.5].

**Story [12.2]: SSO configuration and session management**

As an identity admin,
I want SSO with session controls,
So that users authenticate via trusted providers.

Acceptance Criteria:
1. OIDC/OAuth2 integration; session timeout and refresh policies.
2. MFA enforcement hooks; device/session revocation.
3. Audit of sign‑in/out and session lifecycle.

Prerequisites: [16.4].

**Story [12.3]: Centralized audit pipeline with signed logs**

As a compliance officer,
I want tamper‑evident audit logs,
So that we can prove who did what and when.

Acceptance Criteria:
1. Append‑only log with HMAC signatures; verification tool.
2. Event coverage for auth, policy, data changes, and critical actions.
3. Retention and export controls; privacy filters for sensitive fields.

Prerequisites: [16.8].

**Story [12.4]: Secrets management integration (1Password + ESO)**

As a platform engineer,
I want external secrets synched to Kubernetes,
So that credentials are rotated and injected safely.

Acceptance Criteria:
1. ExternalSecrets Operator pulls from 1Password; mounts to services.
2. Rotation policy documented and tested; no secrets in repo.
3. Audit of read/access events.

Prerequisites: Cluster with ESO; registry configured.

**Story [12.5]: Compliance evidence and controls**

As a compliance officer,
I want evidence capture and mapped controls,
So that audits are efficient.

Acceptance Criteria:
1. Control library mapped to SOC 2/ISO 27001; evidence collection tasks.
2. Automated evidence from logs/dashboards where possible.
3. Gap report with remediation backlog.

Prerequisites: [12.3], [16.8].

---

### Epic 11 — Notifications and Communication

Expanded Goal: Define SLOs, add resiliency middleware, validate HA posture with chaos testing, and enforce error budget gates for risky automation.

Value Proposition: Raise reliability to 99.9% targets with clear budgets and graceful degradation, preventing outages and regression risk.

Stories

**Story [13.1]: SLO definitions and monitoring dashboards**

As an SRE lead,
I want SLOs with dashboards and alerting,
So that reliability is measurable and visible.

Acceptance Criteria:
1. SLOs for API latency, availability, error rate; burn‑rate alerts configured.
2. Dashboards per service; ownership noted.
3. Runbooks linked from alerts.

Prerequisites: [16.8].

**Story [13.2]: Resiliency middleware (timeouts, retries, circuit breakers)**

As a developer,
I want standardized resiliency patterns,
So that transient failures don’t cascade.

Acceptance Criteria:
1. Library/middleware applies timeouts, retries with backoff, and circuit breakers.
2. Defaults per dependency class; metrics exported.
3. Chaos tests verify behavior under fault injection.

Prerequisites: [16.8].

**Story [13.3]: Chaos scenarios and runbooks**

As an SRE,
I want chaos experiments with documented runbooks,
So that teams practice failure handling.

Acceptance Criteria:
1. Scenarios for DB failover, cache outage, policy engine slowness, and event backlog.
2. GameDays planned; results and remediation captured.
3. Runbooks updated with findings.

Prerequisites: [16.6], [16.7], [16.8].

**Story [13.4]: Error budget policy gating for automation**

As a product owner,
I want automation rollouts gated by error budgets,
So that risky features pause when reliability dips.

Acceptance Criteria:
1. Error budget tracking per service; gate toggles policy enforcement (shadow → canary → full).
2. Gate decisions recorded with context and duration.
3. Reports show budget consumption and gate history.

Prerequisites: [16.8], [00.5].

**Story [13.5]: HA posture with degradation health signals**

As an architect,
I want clear health signals for degraded modes,
So that the system fails safe.

Acceptance Criteria:
1. Health endpoints reflect degraded states; UI indicates degraded functionality.
2. Traffic shaping rules for overload; non‑critical features shed load first.
3. Incident playbooks for degraded mode operations.

Prerequisites: [16.5], [16.6], [16.9].

---

### Epic 12 — Security and Compliance

Expanded Goal: Establish a comprehensive test architecture with contract testing, quality gates, traceability, and seeded data to maintain quality as the platform scales.

Value Proposition: Faster, safer changes with clear coverage signals and automated checks preventing regressions.

Stories

**Story [14.1]: Test architecture scaffolding with conventions**

As a developer,
I want standardized test layout and helpers,
So that writing and running tests is straightforward.

Acceptance Criteria:
1. Conventions for unit/integration/e2e; shared fixtures and utilities.
2. Deterministic test runner config with parallelism and retries.
3. Docs for when to write which test.

Prerequisites: —

**Story [14.2]: Contract and event schema tests**

As an integration engineer,
I want contract tests and event schema validation,
So that breaking changes are caught early.

Acceptance Criteria:
1. Spring Cloud Contract (or equivalent) for REST; schema registry/tests for events.
2. CI job fails on incompatible changes; versioning strategy documented.
3. Backward‑compatibility strategy for rolling upgrades.

Prerequisites: [16.5].

**Story [14.3]: Quality gate evaluation service**

As a release manager,
I want gates for coverage, lint, and critical vulnerabilities,
So that poor‑quality builds don’t ship.

Acceptance Criteria:
1. Gates defined with thresholds; status posted to PRs.
2. Exceptions require approval with justification; audit trail kept.
3. Dashboards show trend of gate pass/fail.

Prerequisites: CI configured.

**Story [14.4]: Traceability reporting (requirements → tests → coverage)**

As a QA lead,
I want traceability from requirements to tests and coverage,
So that we know what’s untested.

Acceptance Criteria:
1. Link FR/NFR IDs to tests; coverage report by requirement.
2. Reports highlight gaps; backlog items created automatically.
3. Exportable artifacts for audits.

Prerequisites: [14.1], [14.2].

**Story [14.5]: Data seeding and regression harness**

As a tester,
I want seed data and a regression harness,
So that realistic scenarios are easy to test repeatedly.

Acceptance Criteria:
1. Seed scripts/fixtures for core entities; idempotent and environment‑scoped.
2. Regression suite runs nightly with results trend.
3. Failures auto‑open issues with logs and repro steps.

Prerequisites: [16.9].

---

### Epic 13 — High Availability and Reliability

Expanded Goal: Deploy a policy‑aware API gateway with OpenAPI governance, webhooks, and SDKs so partners can integrate safely and predictably.

Value Proposition: Ecosystem readiness and controlled extensibility without compromising security or performance.

Stories

**Story [15.1]: Gateway deployment with policy configuration (Envoy Gateway)**

As a platform engineer,
I want an API gateway enforcing policies,
So that inbound traffic is secured and shaped consistently.

Acceptance Criteria:
1. Envoy Gateway deployed with routes, rate limits, and authn/z policies.
2. Request/response transformation rules; observability for gateway metrics.
3. Canary rollout procedure documented.

Prerequisites: [16.4], [16.9].

**Story [15.2]: OpenAPI governance with CI checks**

As an API owner,
I want schema governance in CI,
So that breaking changes are prevented.

Acceptance Criteria:
1. Lint rules and breaking‑change detection; versioning strategy enforced.
2. Example requests/responses kept in sync; docs site generation.
3. PR checks block incompatible changes.

Prerequisites: [14.2].

**Story [15.3]: Webhook subscription service with retries and signing**

As an integrator,
I want reliable webhooks with signed payloads,
So that I can receive events securely.

Acceptance Criteria:
1. Subscription CRUD; secret per endpoint; HMAC signatures validated by consumers.
2. Retries with exponential backoff and dead‑letter queues.
3. Delivery metrics and failure alerts.

Prerequisites: [00.1], [16.8].

**Story [15.4]: Event publishing standards and SDKs**

As a developer advocate,
I want standard event schemas and SDKs,
So that partners can publish/consume events easily.

Acceptance Criteria:
1. Canonical event schema docs and JSON Schemas.
2. SDKs or examples for at least one language; quickstart guides.
3. Compatibility tests for producer/consumer conformance.

Prerequisites: [00.1].

**Story [15.5]: API client key and rate limit policy management**

As an admin,
I want API keys/clients with rate limits,
So that access is controlled and fair.

Acceptance Criteria:
1. Client registration and key lifecycle; rotation flows.
2. Rate limit policies per client/route; analytics dashboard.
3. Alerts on abuse or anomalous traffic patterns.

Prerequisites: [11.3], [16.9].

---

## Overview

This document provides the detailed epic breakdown for synergyflow, expanding on the high-level epic list in the PRD.

Each epic includes:

- Expanded goal and value proposition
- Story breakdown with user stories
- Acceptance criteria for each story
- Story sequencing and prerequisites

Epic sequencing principles: Epic 1 establishes foundational infrastructure and initial functionality; subsequent epics build progressively with no forward dependencies.

---

### Epic 14 — Testing and Quality Assurance

Expanded Goal: Deliver the cross-cutting capabilities that build user trust, eliminate duplicate entry and context switching, and make eventual consistency explicit via freshness signals and decision receipts. Establish typed events and auditability to enable safe automation from day one.

Value Proposition: "Magic and audits like a bank" — visible freshness, explainable policy decisions, and seamless cross‑module actions that feel fast and reliable.

Stories

**Story [00.1]: Event System Implementation (FR‑1)**

As a developer,
I want typed ApplicationEvents with durable publication and correlation/causation tracking,
So that services communicate reliably and are observable across module boundaries.

Acceptance Criteria:
1. Events persisted in `event_publication` with retry/status fields (transactional outbox).
2. Correlation and causation IDs propagate across modules and outbound API calls.
3. Consumers are idempotent; integration tests verify no duplicate side-effects.

Prerequisites: None.

**Story [00.2]: Single‑Entry Time Tray (FR‑2)**

As an agent,
I want to log time once and have it mirrored to related Incident and Task worklogs,
So that I avoid duplicate entry and get accurate rollups.

Acceptance Criteria:
1. Global Time Tray accessible from header; optimistic commit with retry on failure.
2. Mirrored writes succeed for ≥2 linked entities; immutable audit trail (user/timestamp).
3. p95 mirror latency ≤200 ms for two destinations under nominal load.

Prerequisites: [00.1].

**Story [00.3]: Link‑on‑Action (FR‑3)**

As an agent,
I want to create related entities in context with prefilled fields and automatic bidirectional links,
So that I can move work forward without context switching.

Acceptance Criteria:
1. Incident → Create Task/Change opens prefilled modal; links Incident↔Task/Change established.
2. Relationship graph exposed via REST; Related panel reflects changes immediately.
3. Authorization enforced; denials show reason with decision receipt link.

Prerequisites: [00.1].

**Story [00.4]: Freshness Badges (FR‑4)**

As a user,
I want visible data freshness on cross‑module reads,
So that I can trust what I see and act confidently.

Acceptance Criteria:
1. Badge shows last projection age with thresholds: green <3s, yellow 3–10s, red >10s.
2. Admin dashboard surfaces p50/p95/p99 projection lag and alerts on >10s for >5m.
3. Internal target p95 projection lag <100 ms for MVP views.

Prerequisites: [00.1].

**Story [00.5]: Policy Studio MVP + Decision Receipts (FR‑5)**

As a policy owner,
I want to manage policies and review explainable decision receipts,
So that automation is governed and auditable.

Acceptance Criteria:
1. Policy CRUD (MVP) and bundle sync to OPA sidecar.
2. Every evaluation stores a signed receipt with policy version, inputs, outcome, and timestamps.
3. Shadow mode supported; canary flag gates enforcement to production.

Prerequisites: [00.1].

---

## Story Guidelines Reference

Story format:

```
**Story [EPIC.N]: [Story Title]**

As a [user type],
I want [goal/desire],
So that [benefit/value].

**Acceptance Criteria:**
1. [Specific testable criterion]
2. [Another specific criterion]
3. [etc.]

**Prerequisites:** [Dependencies on previous stories]
```

Requirements:
- Vertical slices, sequential ordering, no forward dependencies.
- Completable in 2–4 hour focused sessions.
- Value-focused; integrate enabling work into value delivery where possible.

---

### Epic 15 — Integration and API Gateway (Advanced)

Expanded Goal: Deliver core incident lifecycle management with SLA tracking and the groundwork for problem linkage, matching competitor parity while integrating with our event‑driven backbone.

Value Proposition: Replace ManageEngine ServiceDesk Plus incident management with a faster, integrated experience that emits reliable events for automation, analytics, and cross‑module workflows.

Stories

**Story [01.1]: Incident CRUD with priority/severity**

As a service agent,
I want to create, read, update, and delete incidents with priority and severity,
So that I can manage work efficiently and communicate urgency.

Acceptance Criteria:
1. Fields: title, description, priority (Low/Medium/High/Critical), severity (S1–S4), assigned_to, status.
2. Server‑side validation and API + UI endpoints complete with authorization.
3. Audit of changes (who/when) persisted.

Prerequisites: [00.1].

**Story [01.2]: Assignment and manual routing**

As a dispatcher,
I want to assign incidents to agents or teams,
So that work is routed correctly.

Acceptance Criteria:
1. Assignment operations available via API and UI with role checks.
2. Assignment changes recorded; event emitted for assignment updates.
3. Filters by assignee/team and status.

Prerequisites: [01.1].

**Story [01.3]: SLA timer‑based tracking (Flowable timers)**

As a service manager,
I want SLA timers to track due thresholds,
So that I can enforce response/resolution targets.

Acceptance Criteria:
1. Flowable BPMN timers fire within ±5 seconds (p95) of deadline.
2. SLA status visible on incident; breach events recorded and emitted.
3. Configurable per priority/severity profile.

Prerequisites: [01.1].

**Story [01.4]: Incident lifecycle workflow**

As an agent,
I want a guided lifecycle (New → Assigned → In Progress → Resolved → Closed),
So that state transitions are consistent and auditable.

Acceptance Criteria:
1. Valid transitions enforced; reasons required where applicable.
2. Lifecycle transitions publish events via ApplicationEventPublisher.
3. Events persisted to `event_publication` and delivered to consumers p95 ≤100 ms.

Prerequisites: [01.1], [00.1].

**Story [01.5]: Comments, worklog, attachments**

As an agent,
I want to add comments, log work, and attach files to incidents,
So that collaboration and records are complete.

Acceptance Criteria:
1. Comment and worklog entities linked to incident with timestamps and authors.
2. Attachments stored with metadata; size/type validated.
3. Related updates visible in UI and available via API.

Prerequisites: [01.1].

**Story [01.6]: Incident event publication**

As a platform integrator,
I want incident events published for key transitions,
So that downstream consumers can react reliably.

Acceptance Criteria:
1. Events: IncidentCreated, IncidentAssigned, IncidentResolved (minimum set).
  2. Persisted in `event_publication` with retry semantics; consumer delivery p95 ≤100 ms.
  3. Observability: dashboards show event lag, error rate; alerts on anomalies.

  Prerequisites: [00.1].

---

### Epic 16 — Platform Foundation (Deployment and Operations)

Expanded Goal: Provide robust change management with approvals and deployment tracking, enabling safe, auditable delivery while reducing lead time through progressive automation.

Value Proposition: Shorter change cycles with governance — policy‑ready approvals and end‑to‑end visibility from request to deployment.

Stories

**Story [02.1]: Change request CRUD with risk assessment**

As a change requester,
I want to submit and manage change requests with risk classification,
So that changes are standardized and risk‑aware.

Acceptance Criteria:
1. CRUD for changes with fields: title, description, risk (Low/Medium/High/Emergency), service/module, planned window.
2. Validation rules; audit trail for edits and status.
3. API + UI flows complete with authorization.

Prerequisites: [00.1].

**Story [02.2]: Manual approval routing**

As a release manager,
I want to route approvals to designated approvers,
So that governance is enforced prior to deployment.

Acceptance Criteria:
1. Approval steps configurable per risk level; approver assignment and decisions recorded.
2. Status transitions visible; notifications on requested/approved/rejected.
3. Ready to integrate with policy engine in Phase 2.

Prerequisites: [02.1].

**Story [02.3]: Change calendar with conflict detection**

As an operations planner,
I want a calendar that highlights conflicting windows,
So that scheduling avoids downtime and contention.

Acceptance Criteria:
1. Calendar view of scheduled changes; conflicts flagged by CI/service and time overlap.
2. Filters by service, risk, environment; exportable agenda.
3. Conflict API available for automation checks.

Prerequisites: [02.1].

**Story [02.4]: Deployment tracking**

As a release engineer,
I want to track deployment state (Scheduled → In Progress → Completed),
So that stakeholders see real‑time progress.

Acceptance Criteria:
1. State machine with timestamps; audit entries and events on transitions.
2. Rollback status captured; notes and artifacts linked.
3. Dashboard aggregates deployments by status/time window.

Prerequisites: [02.1].

**Story [02.5]: Change event publication**

As a platform integrator,
I want change events emitted for key transitions,
So that downstream systems can react and audit accurately.

Acceptance Criteria:
1. Events: ChangeRequested, ChangeApproved, ChangeDeployed (minimum set).
2. Persisted to `event_publication`; consumer delivery p95 ≤100 ms.
3. Observability dashboards for change event lag and error rate.

Prerequisites: [00.1].

---
