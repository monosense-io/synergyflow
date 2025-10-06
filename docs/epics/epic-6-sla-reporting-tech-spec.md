# Technical Specification: SLA Management & Reporting

Date: 2025-10-06
Author: monosense
Epic ID: 6
Status: Draft (OPTIONAL - Phase 2 Candidate)

---

## Overview

**NOTE: Epic 6 is designated as OPTIONAL in the PRD and is a Phase 2 candidate. It can be deferred if timeline pressure requires reducing from 20 weeks to 18 weeks.**

Epic 6 extends the baseline SLA tracking established in Epic 2 (ITSM) with advanced SLA policy management and analytics reporting. While Epic 2 provides hardcoded priority-based SLA durations (Critical: 2h, High: 4h, Medium: 8h, Low: 24h) and basic SLA compliance metrics in the ITSM dashboard, Epic 6 enables administrators to define custom SLA policies with complex rules (category-based, business hours calendars, multi-tier escalation chains) and provides deep-dive analytics for performance optimization.

The core deliverables are: (1) **SLA Policy Editor** allowing admins to create/edit SLA policies with business rules (e.g., "Network incidents require 1h response, 4h resolution during business hours"), (2) **Advanced Reporting Dashboard** providing drill-down views of SLA compliance by category, agent, time period, with root cause analysis for breaches, and (3) **PDF Export** for executive stakeholder reports. The implementation leverages the existing Timer/SLA Service (Epic 1) and extends it with a policy evaluation engine and OLAP-style reporting queries against historical ticket/SLA data.

This epic addresses organizational maturity needs where generic SLA policies prove insufficient—for example, enterprises with differentiated service levels for VIP users, critical infrastructure categories, or complex approval workflows requiring multi-stage SLA tracking. It also supports continuous improvement by surfacing patterns (e.g., "Network team consistently breaches SLAs on Mondays due to weekend backlog").

## Objectives and Scope

**In Scope:**
- **Custom SLA Policy Management:**
  - Admin UI for creating/editing SLA policies with rule builder (IF category=Network AND priority=Critical THEN response_sla=30min, resolution_sla=2h)
  - Business hours calendar configuration (e.g., 9am-5pm Mon-Fri; SLA clock pauses outside business hours)
  - Multi-tier escalation rules (e.g., if 50% SLA elapsed → notify assignee, if 75% → escalate to manager, if 90% → page director)
  - Policy versioning and audit trail (track when policies changed, who changed them, applied retroactively or prospectively)

- **Advanced SLA Reporting:**
  - **SLA Compliance Dashboard** with drill-down views:
    - Overall compliance rate trend (daily/weekly/monthly) with target line (95% per NFR5)
    - Compliance by category (Network: 92%, Software: 97%, Hardware: 88%)
    - Compliance by agent (Alice: 98%, Bob: 85%, identify training needs)
    - Compliance by priority (Critical: 90%, High: 94%, Medium: 97%, Low: 99%)
  - **Breach Analysis Report:**
    - List of SLA breaches with root cause tagging (delayed assignment, parts unavailable, requires vendor, complex diagnosis)
    - Mean time to breach (MTTB) metric showing average elapsed time before breach
    - Repeat offenders (tickets frequently breached by same agent or category)
  - **Agent Performance Report:**
    - Per-agent metrics: SLA compliance %, mean resolution time, first response time, workload (tickets assigned/resolved)
    - Comparative leaderboard for gamification (top 5 agents by SLA compliance)
  - **Historical Trend Analysis:**
    - SLA compliance trends over 6 months (identify improving/degrading categories)
    - Seasonal patterns (SLA breaches spike after major releases, holiday weekends)

- **Export and Sharing:**
  - Export any report to PDF with company branding (logo, header/footer)
  - Export raw data to CSV/Excel for offline analysis
  - Schedule automated email delivery of reports (e.g., weekly SLA compliance summary to executives every Monday 8am)

- **Integration with Existing ITSM:**
  - SLA policy evaluation integrated into ticket creation/update flows (Epic 2)
  - Timer/SLA Service (Epic 1) updated to support dynamic policy lookup instead of hardcoded durations
  - ITSM Agent Console (Epic 2) displays active SLA policy name/rules for each ticket

**Out of Scope (deferred to Phase 3 or never):**
- Predictive SLA analytics (ML models forecasting breach probability)
- Cross-module SLA tracking (PM issues with SLAs; Epic 6 focuses on ITSM only)
- SLA credits or financial penalties (no billing integration)
- External SLA reporting APIs (no third-party tool integration like ServiceNow)
- Real-time SLA breach alerting via SMS/phone call (email/in-app only)

**Explicitly Not Included:**
- Custom SLA policies for PM issues (PM module operates on sprint-based deadlines, not SLA timers)
- SLA tracking for workflow approvals (approval response time tracked separately in Epic 4)
- Multi-tenant SLA isolation (single-tenant architecture per Phase 1 scope)

## System Architecture Alignment

Epic 6 extends the existing **Timer/SLA Service** (Companion) and adds a new **Reporting Module** within the Core App modulith.

**Module Integration:**
- **Reporting Module (new):** Implements SLA policy CRUD, analytics queries, PDF generation. Exposes `@NamedInterface ReportingService` with methods: `generateComplianceReport(filters)`, `exportToPdf(reportId)`. Consumes ITSM module's ticket/SLA historical data.
- **Timer/SLA Service (existing, Epic 1):** Enhanced to support dynamic policy lookup. New method: `evaluatePolicy(ticketId) → SlaPolicyResult{response_sla, resolution_sla, escalation_rules}`. Replaces hardcoded durations with database-backed policy engine.
- **ITSM Module (Epic 2):** Integrates SLA policy evaluation on ticket create/update. Ticket detail view displays active SLA policy name and rules. Agent Console shows policy-specific countdown indicators.
- **Security Module (existing):** RBAC enforcement for SLA policy editor (Admin role only) and reporting (Admin + Manager roles can view all reports; Agents see only their own performance).

**Data Architecture:**
- **New Tables:**
  - `sla_policies`: `{id, name, description, rules_json, business_hours_config, created_at, created_by, active}`
  - `sla_policy_versions`: `{policy_id, version, rules_json, valid_from, valid_to, changed_by}` (audit trail for policy changes)
  - `sla_breach_log`: `{id, ticket_id, policy_id, sla_type, due_at, breached_at, breach_reason, root_cause_tag}` (denormalized for fast analytics)

- **Enhanced Tables:**
  - `sla_tracking` (existing from Epic 2): Add column `policy_id` (foreign key to `sla_policies`) to track which policy applied
  - `tickets` (existing): Add column `active_policy_id` for quick policy lookup

**Deployment Alignment:**
- **Core App (HTTP):** Reporting Module runs within modulith; no new deployment unit
- **Timer/SLA Service:** Existing companion updated with policy evaluation logic; no scaling changes (still 2 replicas)
- **PostgreSQL:** New tables for policies + breach log; estimated +500MB storage for 1 year of breach history (10k tickets * 10% breach rate * 5KB/record)
- **PDF Generation:** Leverage Thymeleaf templates + Flying Saucer (HTML→PDF library); server-side rendering in reporting module; no headless browser needed

**Performance Considerations:**
- **Policy Evaluation:** O(1) lookup from cached policy map (Redis cache with 5-minute TTL); fallback to database on cache miss
- **Analytics Queries:** OLAP-style queries against denormalized `sla_breach_log` table with indexes on `{category, created_at}`, `{agent_id, created_at}`, `{policy_id, breached_at}`; expect p95 <1s for 6-month trend queries
- **PDF Generation:** Synchronous for small reports (<10 pages, <5s), async job queue for large reports (>50 pages, 30s-2min) with email notification on completion

## Detailed Design

### Services and Modules

| Service/Module | Responsibilities | Inputs | Outputs | Owner |
|---|---|---|---|---|
| **SlaPolicyService** (new) | CRUD operations for SLA policies; policy versioning; rule validation (JSON schema checks) | Policy create/update DTOs with rules JSON | Persisted SLA policy entity; validation errors | Backend (Reporting Module) |
| **SlaPolicyEvaluator** (new) | Evaluates which SLA policy applies to a ticket based on rules (category, priority, requester); caches policies in Redis | Ticket attributes: `{category, priority, requester, created_at}` | `SlaPolicyResult{response_sla_minutes, resolution_sla_minutes, escalation_rules, policy_id}` | Backend (Timer/SLA Service) |
| **BusinessHoursCalculator** (new) | Calculates SLA due dates accounting for business hours (9am-5pm Mon-Fri); pauses timer outside business hours | `start_time`, `sla_duration_minutes`, `business_hours_config` | `due_at` (Instant adjusted for business hours) | Backend (Timer/SLA Service) |
| **ReportingService** (new) | Generates SLA compliance reports, breach analysis, agent performance; aggregates data from `sla_breach_log` and `tickets` | Report type, filters: `{date_range, category, agent, priority}` | `ReportDto` with charts data, summary stats | Backend (Reporting Module) |
| **PdfExportService** (new) | Converts report HTML (Thymeleaf template) to PDF using Flying Saucer; supports branding (logo, header/footer) | `ReportDto`, template name, company branding config | PDF byte array (streamed to client or saved to storage for email) | Backend (Reporting Module) |
| **BreachLogWriter** (new) | Writes to `sla_breach_log` table when SLA breaches occur; event listener consumes `TicketSlaBreached` events from outbox | Event: `{ticket_id, policy_id, sla_type, due_at, breached_at}` | Persisted breach log row with root cause tag (manual or auto-tagged) | Backend (Reporting Module) |
| **SlaPolicyEditorUI** (new) | React component for admin SLA policy CRUD; visual rule builder (drag-drop conditions: IF category=X THEN sla=Y) | User selections from dropdowns/inputs | REST calls to SlaPolicyService API | Frontend (Shared UI) |
| **SlaReportDashboardUI** (new) | React component rendering compliance charts (line/bar/pie via Recharts); drill-down views with filters (category, agent, date range) | Report data from ReportingService | Interactive charts, export PDF/CSV buttons | Frontend (Shared UI) |

**Module Boundaries:**
- Reporting Module exposes `@NamedInterface ReportingService` with methods: `generateComplianceReport(filters)`, `generateBreachAnalysis(filters)`, `generateAgentPerformance(filters)`, `exportToPdf(reportId)`.
- Timer/SLA Service enhanced with `SlaPolicyEvaluator` interface consumed by ITSM module on ticket creation: `ItsmModule → SlaPolicyEvaluator.evaluate(ticket) → SlaPolicyResult`.
- Breach log writes triggered by event listener (decoupled): ITSM emits `TicketSlaBreached` event → Reporting Module consumes → writes breach log (async, no coupling).

### Data Models and Contracts

**New Database Tables:**

```sql
-- SLA Policies
CREATE TABLE sla_policies (
  id UUID PRIMARY KEY DEFAULT gen_uuidv7(),
  name VARCHAR(255) NOT NULL,
  description TEXT,
  rules_json JSONB NOT NULL,  -- {conditions: [], sla_durations: {}, escalation_rules: []}
  business_hours_config JSONB,  -- {timezone: "America/New_York", hours: [{day: "MON", start: "09:00", end: "17:00"}]}
  active BOOLEAN DEFAULT true,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  created_by UUID REFERENCES users(id),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_sla_policies_active ON sla_policies(active);

-- SLA Policy Versions (audit trail)
CREATE TABLE sla_policy_versions (
  id UUID PRIMARY KEY DEFAULT gen_uuidv7(),
  policy_id UUID NOT NULL REFERENCES sla_policies(id),
  version INT NOT NULL,
  rules_json JSONB NOT NULL,
  valid_from TIMESTAMPTZ NOT NULL,
  valid_to TIMESTAMPTZ,  -- NULL for current version
  changed_by UUID REFERENCES users(id),
  change_reason TEXT,
  UNIQUE (policy_id, version)
);

-- SLA Breach Log (denormalized for analytics)
CREATE TABLE sla_breach_log (
  id UUID PRIMARY KEY DEFAULT gen_uuidv7(),
  ticket_id UUID NOT NULL REFERENCES tickets(id),
  policy_id UUID REFERENCES sla_policies(id),
  sla_type VARCHAR(50) NOT NULL,  -- 'RESPONSE' or 'RESOLUTION'
  due_at TIMESTAMPTZ NOT NULL,
  breached_at TIMESTAMPTZ NOT NULL,
  breach_duration_minutes INT NOT NULL,  -- how long past due
  root_cause_tag VARCHAR(100),  -- 'delayed_assignment', 'parts_unavailable', 'vendor_dependency', etc.
  category VARCHAR(100),  -- denormalized from ticket for fast queries
  priority VARCHAR(50),   -- denormalized
  agent_id UUID,          -- denormalized
  created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_breach_log_category_date ON sla_breach_log(category, created_at);
CREATE INDEX idx_breach_log_agent_date ON sla_breach_log(agent_id, created_at);
CREATE INDEX idx_breach_log_policy_breached ON sla_breach_log(policy_id, breached_at);

-- Enhanced existing table
ALTER TABLE sla_tracking ADD COLUMN policy_id UUID REFERENCES sla_policies(id);
ALTER TABLE tickets ADD COLUMN active_policy_id UUID REFERENCES sla_policies(id);
```

**SLA Policy Rules JSON Schema:**

```json
{
  "conditions": [
    {"field": "category", "operator": "equals", "value": "Network"},
    {"field": "priority", "operator": "in", "value": ["CRITICAL", "HIGH"]}
  ],
  "sla_durations": {
    "response_minutes": 30,
    "resolution_minutes": 120
  },
  "escalation_rules": [
    {"threshold_pct": 50, "action": "notify_assignee"},
    {"threshold_pct": 75, "action": "escalate_to_manager"},
    {"threshold_pct": 90, "action": "page_director"}
  ]
}
```

**Backend DTOs:**

```java
// SLA Policy Create/Update DTO
public record SlaPolicyDto(
    UUID id,
    String name,
    String description,
    JsonNode rulesJson,  // Validated against JSON schema
    JsonNode businessHoursConfig,
    boolean active
) {}

// SLA Compliance Report DTO
public record SlaComplianceReportDto(
    LocalDate startDate,
    LocalDate endDate,
    double overallComplianceRate,  // 0.0-1.0
    List<ComplianceByCategoryDto> byCategory,
    List<ComplianceByAgentDto> byAgent,
    List<ComplianceByPriorityDto> byPriority,
    List<TrendDataPoint> trendData  // Daily compliance % over date range
) {}

public record ComplianceByCategoryDto(
    String category,
    int totalTickets,
    int slaCompliant,
    int slaBreached,
    double complianceRate
) {}

// Breach Analysis DTO
public record BreachAnalysisDto(
    List<SlaBreachDto> breaches,
    Map<String, Integer> rootCauseDistribution,  // {"delayed_assignment": 45, "parts_unavailable": 30}
    double meanTimeToBreachMinutes,
    List<RepeatOffenderDto> repeatOffenders  // Agents/categories with high breach rates
) {}

public record SlaBreachDto(
    UUID ticketId,
    String ticketTitle,
    String category,
    String priority,
    String agentName,
    Instant dueAt,
    Instant breachedAt,
    int breachDurationMinutes,
    String rootCauseTag
) {}
```

### APIs and Interfaces

**SLA Policy Management APIs:**

**GET /api/sla/policies**
- **Description:** List all SLA policies (active and inactive)
- **Auth:** Admin role required
- **Query Params:** `?active=true` (filter active only)
- **Response 200:**
  ```json
  [
    {
      "id": "pol-123",
      "name": "Critical Network SLA",
      "description": "30min response, 2h resolution for critical network issues",
      "rulesJson": {...},
      "businessHoursConfig": {...},
      "active": true,
      "createdAt": "2025-10-01T10:00:00Z"
    }
  ]
  ```
- **Performance:** p95 <100ms (queries `sla_policies` table, typically <50 rows)

**POST /api/sla/policies**
- **Description:** Create new SLA policy
- **Auth:** Admin role required
- **Request Body:** `SlaPolicyDto` (without id)
- **Validation:** JSON schema validation for `rulesJson` (must have valid conditions, sla_durations, escalation_rules)
- **Response 201:** Created policy with generated `id`
- **Errors:** 400 (invalid rules JSON), 403 (not admin), 409 (duplicate name)

**PUT /api/sla/policies/{id}**
- **Description:** Update existing SLA policy (creates new version in `sla_policy_versions`)
- **Auth:** Admin role required
- **Request Body:** `SlaPolicyDto`
- **Side Effect:** Inserts row into `sla_policy_versions` with incremented version number; updates `valid_to` of previous version
- **Response 200:** Updated policy
- **Errors:** 404 (policy not found), 400 (invalid rules), 403 (not admin)

**DELETE /api/sla/policies/{id}**
- **Description:** Soft delete (set `active=false`); cannot delete if policy is referenced by active tickets
- **Auth:** Admin role required
- **Response 204:** No content
- **Errors:** 409 (policy in use by active tickets), 404 (policy not found)

**Reporting APIs:**

**GET /api/reports/sla-compliance**
- **Description:** Generate SLA compliance report with drill-down views
- **Auth:** Admin or Manager role required
- **Query Params:**
  - `startDate` (ISO 8601 date, required)
  - `endDate` (ISO 8601 date, required)
  - `category` (optional filter)
  - `agentId` (optional filter)
  - `priority` (optional filter)
- **Response 200:** `SlaComplianceReportDto` with charts data
- **Performance:** p95 <1s for 6-month queries (indexed queries on `sla_breach_log`)
- **Errors:** 400 (invalid date range >1 year), 403 (not authorized)

**GET /api/reports/breach-analysis**
- **Description:** Generate breach analysis report with root cause distribution
- **Auth:** Admin or Manager role required
- **Query Params:** Same as compliance report
- **Response 200:** `BreachAnalysisDto`
- **Performance:** p95 <1s
- **Errors:** 400, 403

**GET /api/reports/agent-performance**
- **Description:** Generate agent performance leaderboard
- **Auth:** Admin or Manager (view all agents); Agent (view own only)
- **Query Params:** `startDate`, `endDate`, `agentId` (optional, agents can only query self)
- **Response 200:** List of `AgentPerformanceDto` sorted by compliance rate desc
- **Performance:** p95 <1s
- **Errors:** 403 (agent querying other agent's data)

**POST /api/reports/{reportId}/export/pdf**
- **Description:** Export report to PDF
- **Auth:** Admin or Manager required
- **Request Body:** `{reportType, filters, branding: {logoUrl, companyName}}`
- **Response 200:** PDF byte stream (`application/pdf`)
- **Performance:** Sync for <10 pages (<5s), async job for >10 pages (returns 202 Accepted with job_id for status polling)
- **Errors:** 404 (report not found), 500 (PDF generation failed)

**GET /api/reports/export/{jobId}**
- **Description:** Poll async PDF export job status
- **Response 200:** `{status: "IN_PROGRESS" | "COMPLETED" | "FAILED", downloadUrl}`
- **Response 202:** Job still running (retry after 5s)

### Workflows and Sequencing

**Sequence 1: Create Custom SLA Policy**

```
Admin → SlaPolicyEditorUI.createPolicy()
SlaPolicyEditorUI → Build rules JSON from UI selections (IF category=Network AND priority=Critical THEN response=30min, resolution=2h)
SlaPolicyEditorUI → POST /api/sla/policies with SlaPolicyDto
API Gateway → SlaPolicyController
SlaPolicyController → SecurityFilter.validateRole(Admin)
SlaPolicyController → SlaPolicyService.createPolicy(dto)
SlaPolicyService → Validate rules JSON against schema (JsonSchemaValidator)
SlaPolicyService → Insert into sla_policies table
SlaPolicyService → Insert initial version into sla_policy_versions (version=1, valid_from=NOW(), valid_to=NULL)
SlaPolicyService → Emit event SlaPolicyCreated to outbox
PostgreSQL ← COMMIT
SlaPolicyService ← return created policy entity
SlaPolicyController ← return 201 Created with policy JSON
Admin ← Policy created, show success toast
```

**Sequence 2: Evaluate SLA Policy on Ticket Creation**

```
Agent → Creates new ticket via ITSM UI (category=Network, priority=Critical)
ITSM API → TicketService.createTicket(dto)
TicketService → SlaPolicyEvaluator.evaluatePolicy(ticket attributes)
  SlaPolicyEvaluator → Check Redis cache for policies (key: "sla_policies:active", TTL 5min)
  SlaPolicyEvaluator → If cache miss → query sla_policies WHERE active=true
  SlaPolicyEvaluator → Filter policies by conditions (find first match for category=Network AND priority=Critical)
  SlaPolicyEvaluator ← return SlaPolicyResult{response_sla: 30min, resolution_sla: 120min, policy_id: pol-123}
TicketService → Store active_policy_id=pol-123 in tickets table
TicketService → Create SLA tracking row with policy_id
TicketService → Call BusinessHoursCalculator.calculateDueDate(start=NOW(), duration=30min, business_hours_config)
  BusinessHoursCalculator → Parse business hours (9am-5pm Mon-Fri)
  BusinessHoursCalculator → If current time is 4:50pm Friday → due_at = Monday 9:20am (30min into Monday)
  BusinessHoursCalculator ← return due_at (Instant)
TicketService → Insert sla_tracking row with due_at
TicketService → Call Timer/SLA Service to schedule escalation timers (at 50%, 75%, 90% thresholds)
TicketService ← ticket created with SLA policy applied
ITSM UI ← Ticket detail view shows "SLA Policy: Critical Network SLA (30min response, 2h resolution, business hours)"
```

**Sequence 3: SLA Breach Logging**

```
Timer/SLA Service → SLA resolution due_at passes without ticket closure
Timer/SLA Service → Emit event TicketSlaBreached to outbox {ticket_id, policy_id, sla_type: RESOLUTION, due_at, breached_at: NOW()}
Event Worker → Poll outbox, publish to Redis Stream
BreachLogWriter (event listener) → Consume TicketSlaBreached event
BreachLogWriter → Fetch ticket details (category, priority, agent_id) from tickets table
BreachLogWriter → Calculate breach_duration_minutes = (breached_at - due_at)
BreachLogWriter → Auto-tag root cause if patterns detected (e.g., if ticket unassigned for >1h before breach → root_cause_tag: "delayed_assignment")
BreachLogWriter → Insert into sla_breach_log table
PostgreSQL ← COMMIT
BreachLogWriter → Send notification to agent + manager: "Ticket #123 breached SLA by 15 minutes"
```

**Sequence 4: Generate SLA Compliance Report**

```
Manager → Navigate to SLA Reports UI
SlaReportDashboardUI → Select date range (last 30 days), category filter (Network)
SlaReportDashboardUI → GET /api/reports/sla-compliance?startDate=2025-09-06&endDate=2025-10-06&category=Network
API Gateway → ReportingController
ReportingController → SecurityFilter.validateRole(Admin | Manager)
ReportingController → ReportingService.generateComplianceReport(filters)
ReportingService → Query sla_breach_log for breach count grouped by category, agent, priority
ReportingService → Query tickets for total ticket count in date range
ReportingService → Calculate compliance rate: (total_tickets - breaches) / total_tickets
ReportingService → Build trend data: GROUP BY DATE(created_at), calculate daily compliance rate
ReportingService ← return SlaComplianceReportDto
ReportingController ← return 200 OK with JSON
SlaReportDashboardUI ← Render charts:
  - Line chart: Daily compliance % trend (Recharts <LineChart>)
  - Bar chart: Compliance by category (Recharts <BarChart>)
  - Pie chart: Breach root causes (Recharts <PieChart>)
  - Table: Agent performance leaderboard
Manager → Click "Export to PDF"
SlaReportDashboardUI → POST /api/reports/export/pdf {reportType: SLA_COMPLIANCE, filters, branding}
API Gateway → PdfExportService
PdfExportService → Render Thymeleaf template with report data (company logo, header/footer)
PdfExportService → Flying Saucer: HTML → PDF conversion
PdfExportService ← return PDF byte array
Manager ← Download PDF file (compliance-report-2025-10-06.pdf)
```

**Error Handling:**
- **Policy Evaluation Failure:** If no matching policy found → fall back to default hardcoded SLA (Epic 2 behavior); log warning for admin review
- **Breach Log Write Failure:** Retry 3x with backoff; if still fails, emit dead-letter event for manual investigation; SLA breach notification still sent to avoid missed alerts
- **PDF Export Timeout:** If PDF generation >30s → move to async job queue, return 202 Accepted with job_id; send email when PDF ready
- **Invalid Date Range:** If date range >1 year → return 400 "Date range too large, max 1 year"

## Non-Functional Requirements

### Performance

**API Latency:**
- SLA Policy CRUD: p95 <200ms (lightweight table with <50 rows)
- Policy Evaluation: p95 <50ms (Redis cache hit), p95 <100ms (cache miss + DB query)
- Business Hours Calculation: p95 <10ms (pure computation, no I/O)
- Compliance Report Query: p95 <1s for 6-month date range (indexed OLAP queries on `sla_breach_log`)
- Breach Analysis Query: p95 <1s
- Agent Performance Query: p95 <500ms (indexed by agent_id + date)
- PDF Export (sync): p95 <5s for reports <10 pages (HTML rendering + Flying Saucer conversion)
- PDF Export (async): <2min for reports 10-50 pages (background job queue)

**Data Volume Sizing:**
- `sla_policies`: ~50 policies at scale (org with multiple categories, priorities, SLAs)
- `sla_policy_versions`: ~200 versions over 2 years (4 policy changes/year * 50 policies)
- `sla_breach_log`: ~10k breaches/year (10k tickets * 10% breach rate); 6-year retention for compliance = 60k rows
- Storage estimate: 500MB for 6 years of breach history + policy versions

**Cache Performance:**
- Redis SLA policy cache: Key `sla_policies:active`, 5-minute TTL, <100KB payload (all active policies)
- Cache hit rate target: >95% (policies change infrequently)
- Cache invalidation: Evict on policy create/update/delete (explicit EXPIRE command)

### Security

**Authentication & Authorization (aligned with PRD NFR12):**
- **SLA Policy Editor:** Admin role only (RBAC check on all POST/PUT/DELETE endpoints)
- **Reporting APIs:**
  - Admin + Manager: Can view all reports (all categories, all agents)
  - Agent: Can view only own performance report (403 if querying other agent's data)
- **PDF Export:** Admin + Manager only (contains sensitive performance data)

**Data Protection:**
- SLA breach log contains no PII (ticket titles excluded; only IDs and tags)
- Agent performance reports display aggregate metrics only (no individual ticket details in exported PDFs unless drill-down)
- Root cause tags sanitized (no free-text user input; controlled vocabulary to prevent injection)

**Audit Trail:**
- `sla_policy_versions` table tracks all policy changes (who, when, what changed)
- Audit log records: Policy create/update/delete, report generation (who ran report, when, filters applied)

### Reliability/Availability

**Availability (aligned with PRD NFR3):**
- Target: 99.9% availability (inherits from Core App)
- Reporting queries tolerate stale data (5-minute cache staleness acceptable; reports are retrospective, not real-time)

**Resilience:**
- **Policy Evaluation Failure:** Fall back to default hardcoded SLA (Epic 2 behavior); log warning for admin review; ticket creation not blocked
- **Breach Log Write Failure:** Retry 3x with exponential backoff; emit dead-letter event if still fails; SLA breach notification still sent (decoupled)
- **PDF Generation Failure:** Return 500 with error message; user can retry; async jobs retry 2x before marking failed
- **Report Query Timeout:** If query >10s → return 504 Gateway Timeout with suggestion to narrow date range

**Data Integrity:**
- Policy versioning prevents data loss on edits (old versions preserved in `sla_policy_versions`)
- Breach log denormalized (category, priority, agent_id) to survive ticket deletions (immutable audit record)

### Observability

**Metrics:**
- **Policy Evaluation Rate:** Counter tracking evaluations/sec (baseline 1-5/sec during ticket creation bursts)
- **Policy Cache Hit Rate:** Gauge tracking Redis cache hit % (target >95%)
- **Report Generation Latency:** Histogram for each report type (compliance, breach analysis, agent performance)
- **PDF Export Queue Depth:** Gauge for async job queue (alert if >50 pending PDFs)
- **Breach Log Write Rate:** Counter tracking breaches/hour (baseline 1-2/hour, spike indicates SLA issues)

**Traces (OpenTelemetry):**
- Policy evaluation: `TicketService.createTicket` → `SlaPolicyEvaluator.evaluate` → cache/DB span
- Report generation: `ReportingController.generateReport` → DB query spans → aggregation logic
- PDF export: Template rendering span → Flying Saucer conversion span

**Logs (Structured JSON):**
- Policy create/update/delete: `{event: "sla_policy_modified", policyId, policyName, changedBy, version}`
- Policy evaluation: `{event: "sla_policy_evaluated", ticketId, policyId, slaResult}` (sample 10%)
- Breach logged: `{event: "sla_breach_logged", ticketId, policyId, breachDurationMinutes, rootCauseTag}`
- Report generated: `{event: "report_generated", reportType, filters, generatedBy, latency}`
- PDF export: `{event: "pdf_exported", reportType, pageCounts, latency, async}`

**Alerts:**
- Policy cache hit rate <90% for 10 minutes → "SLA policy cache degraded"
- Report query latency p95 >2s for 5 minutes → "SLA reporting slow"
- Breach log write failures >5 in 10 minutes → "SLA breach logging failing"
- PDF export queue depth >50 for 10 minutes → "PDF export queue backed up"

## Dependencies and Integrations

**Epic Dependencies (Blockers):**
- **Epic 1 (Foundation):** MUST be complete for PostgreSQL, Redis cache, Timer/SLA Service infrastructure
- **Epic 2 (ITSM):** MUST be complete for ticket/SLA tracking tables, ITSM module integration
- **Epic 5 (Dashboard):** SHOULD be complete for real-time breach notifications via SSE (optional; email fallback available)

**Backend Technology Stack:**

| Component | Version | Purpose | Notes |
|---|---|---|---|
| Spring Boot | 3.5.0 | Core framework | Reporting Module within modulith |
| JSON Schema Validator | 1.5.1 | Policy rules validation | Validates `rulesJson` against schema |
| Thymeleaf | Boot-managed | HTML template engine | Renders report HTML for PDF |
| Flying Saucer (OpenHTMLToPDF) | 1.0.10 | HTML→PDF conversion | Server-side rendering, no headless browser |
| Recharts (frontend) | ^2.8.0 | Charting library | Line/bar/pie charts for compliance dashboard |
| PostgreSQL JSONB | Built-in | Policy rules storage | Indexed JSONB queries for policy evaluation |
| Redis | 7.2.x | Policy cache | Cache active policies for fast lookup |

**Infrastructure Dependencies:**
- **PostgreSQL:** New tables `sla_policies`, `sla_policy_versions`, `sla_breach_log`; estimated +500MB storage
- **Redis:** Policy cache (key: `sla_policies:active`, <100KB payload)
- **Email Service (existing from Epic 1):** Send PDF reports via scheduled email (SMTP integration assumed from Foundation)

**External Integrations:**
- **None:** No third-party services in Phase 1 (no Slack/ServiceNow integrations)

**Internal Module Integrations:**
- **ITSM Module:** Calls `SlaPolicyEvaluator.evaluate(ticket)` on ticket create; displays policy name in Agent Console
- **Timer/SLA Service:** Enhanced with `SlaPolicyEvaluator` and `BusinessHoursCalculator`; emits `TicketSlaBreached` events
- **Reporting Module (new):** Consumes `TicketSlaBreached` events via `BreachLogWriter`; exposes `@NamedInterface ReportingService`
- **Security Module:** RBAC enforcement for policy editor (Admin only) and reports (Admin/Manager/Agent-own-data)

## Acceptance Criteria (Authoritative)

**NOTE:** Epic 6 is optional. These AC would apply if Epic 6 is implemented.

**AC-6.1: Create Custom SLA Policy**
- **Given** an admin user on SLA Policy Editor page
- **When** the admin creates policy "Critical Network SLA" with rules: IF category=Network AND priority=Critical THEN response_sla=30min, resolution_sla=2h, business_hours=9am-5pm Mon-Fri
- **Then** the policy saves successfully, appears in policy list as active, and version 1 is created in audit table

**AC-6.2: Policy Evaluation on Ticket Creation**
- **Given** active policy "Critical Network SLA" exists
- **When** agent creates ticket with category=Network, priority=Critical at 4:50pm Friday
- **Then** ticket is assigned policy_id="Critical Network SLA", SLA due date is Monday 9:20am (30min into business hours), and ticket detail shows policy name and rules

**AC-6.3: Business Hours SLA Calculation**
- **Given** policy with business hours 9am-5pm Mon-Fri, resolution_sla=2h
- **When** ticket created at 4:00pm Friday
- **Then** SLA due date is Monday 10:00am (skips weekend, calculates 2h from Monday 9am)

**AC-6.4: Policy Versioning on Update**
- **Given** existing policy "Critical Network SLA" version 1
- **When** admin updates response_sla from 30min to 45min
- **Then** new version 2 created in `sla_policy_versions`, previous version marked `valid_to=NOW()`, existing tickets retain old policy (no retroactive change)

**AC-6.5: SLA Compliance Report**
- **Given** 30 days of ticket data with 100 tickets, 8 SLA breaches
- **When** manager requests SLA Compliance Report for last 30 days, category=Network
- **Then** report shows overall compliance rate 92%, daily trend chart, compliance by agent table, all rendered within 1 second

**AC-6.6: Breach Analysis with Root Cause**
- **Given** 8 SLA breaches in breach log with auto-tagged root causes
- **When** manager requests Breach Analysis Report
- **Then** report shows breach list with root cause distribution pie chart (e.g., "delayed_assignment": 50%, "parts_unavailable": 25%), mean time to breach metric

**AC-6.7: Agent Performance Leaderboard**
- **Given** 5 agents with varying SLA compliance rates (Alice: 98%, Bob: 85%, Charlie: 92%, Diana: 90%, Eve: 88%)
- **When** manager requests Agent Performance Report
- **Then** report shows leaderboard sorted by compliance rate desc (Alice #1, Bob #5), with metrics: compliance %, mean resolution time, first response time

**AC-6.8: PDF Export (Sync)**
- **Given** generated SLA Compliance Report with 8 pages
- **When** manager clicks "Export to PDF" with company logo
- **Then** PDF downloads within 5 seconds with branding (logo, header/footer), charts rendered as images

**AC-6.9: PDF Export (Async)**
- **Given** large Breach Analysis Report with 60 pages (1000 breach details)
- **When** manager requests PDF export
- **Then** API returns 202 Accepted with job_id, manager polls job status, receives email when PDF ready (within 2 minutes)

**AC-6.10: Policy Cache Performance**
- **Given** 50 active SLA policies cached in Redis
- **When** 100 concurrent ticket creations evaluate policies
- **Then** 95%+ cache hits, p95 policy evaluation latency <50ms (cache hit) or <100ms (cache miss)

**AC-6.11: Scheduled Email Reports**
- **Given** admin configures weekly SLA Compliance Report to email executives every Monday 8am
- **When** Monday 8am arrives
- **Then** system auto-generates report for previous week, exports to PDF, sends email to recipient list with attachment

**AC-6.12: RBAC for Reports**
- **Given** agent user (not Admin/Manager) attempts to access Compliance Report
- **When** agent navigates to /reports/sla-compliance
- **Then** API returns 403 Forbidden (agents can only view own performance)
- **When** agent requests own performance report with agentId=self
- **Then** API returns 200 OK with own metrics only

## Traceability Mapping

| AC ID | Spec Section(s) | Component(s)/API(s) | Test Idea |
|---|---|---|---|
| AC-6.1 | Services (SlaPolicyService), APIs (POST /api/sla/policies), Data Models (sla_policies, sla_policy_versions) | `SlaPolicyController`, `SlaPolicyService`, JSON schema validator | Integration test: POST policy with valid rules → assert 201, query DB for policy + version 1 |
| AC-6.2 | Workflows (Sequence 2: Policy Evaluation), Services (SlaPolicyEvaluator) | `TicketService.createTicket()`, `SlaPolicyEvaluator.evaluate()`, `BusinessHoursCalculator` | Integration test: Create ticket with matching policy conditions → assert policy_id stored, SLA due_at correct |
| AC-6.3 | Services (BusinessHoursCalculator), Data Models (business_hours_config) | `BusinessHoursCalculator.calculateDueDate()` | Unit test: Given Friday 4pm + 2h SLA + business hours 9-5 Mon-Fri → assert due_at = Monday 10am |
| AC-6.4 | Services (SlaPolicyService), Data Models (sla_policy_versions) | `SlaPolicyService.updatePolicy()`, versioning logic | Integration test: Update policy → assert version 2 created, version 1 valid_to set, existing tickets unaffected |
| AC-6.5 | Services (ReportingService), APIs (GET /api/reports/sla-compliance) | `ReportingService.generateComplianceReport()`, OLAP queries | Integration test: Seed 100 tickets + 8 breaches → query report → assert 92% compliance, trend data correct |
| AC-6.6 | Services (ReportingService), APIs (GET /api/reports/breach-analysis), Data Models (sla_breach_log.root_cause_tag) | `ReportingService.generateBreachAnalysis()` | Integration test: Seed breach log with root cause tags → query report → assert distribution pie chart data |
| AC-6.7 | Services (ReportingService), APIs (GET /api/reports/agent-performance) | `ReportingService.generateAgentPerformance()` | Integration test: Seed tickets for 5 agents → query report → assert leaderboard order by compliance desc |
| AC-6.8 | Services (PdfExportService), APIs (POST /api/reports/export/pdf) | `PdfExportService.export()`, Flying Saucer HTML→PDF | Integration test: Generate report → export PDF → assert PDF size <5MB, latency <5s, branding present |
| AC-6.9 | Services (PdfExportService async), APIs (GET /api/reports/export/{jobId}) | Async job queue, email notification | Integration test: Request large PDF → poll job status → assert 202 → 200 COMPLETED with download URL |
| AC-6.10 | Services (SlaPolicyEvaluator), Performance (cache hit rate) | Redis cache, policy lookup logic | Load test: 100 concurrent ticket creates → measure cache hit rate >95%, p95 latency <100ms |
| AC-6.11 | Scheduled email (future enhancement - out of Epic 6 MVP scope) | Scheduled job runner (e.g., Spring @Scheduled) | E2E test: Mock time to Monday 8am → verify report generated + email sent (or mark as Phase 2) |
| AC-6.12 | APIs (RBAC enforcement), Security (role checks) | `ReportingController` security filters | Integration test: Call report API as Agent → assert 403; call own performance → assert 200 with own data only |

## Risks, Assumptions, Open Questions

**Risks:**

1. **Risk: Policy Rule Complexity Explosion**
   - **Impact:** Admins create overly complex policies with 10+ nested conditions (IF category=X AND priority=Y AND requester.department=Z...), making evaluation slow and error-prone
   - **Likelihood:** Medium (power users tend to over-engineer rules)
   - **Mitigation:** Limit policy conditions to max 5 per policy (UI validation); simplify UX with templates ("Critical Infrastructure SLA" pre-configured); provide policy simulation tool to test before activating
   - **Owner:** Product Manager + UX Expert

2. **Risk: Business Hours Calculation Edge Cases**
   - **Impact:** SLA due dates incorrectly calculated for edge cases (holidays, daylight saving time transitions, leap years), causing breach notifications to fire prematurely or late
   - **Likelihood:** Medium (business hours logic is complex)
   - **Mitigation:** Use battle-tested library (e.g., Joda-Time BusinessCalendar or custom with extensive unit tests); document known limitations (e.g., holidays not supported in Phase 1; admin manually adjusts SLAs for holidays)
   - **Owner:** Backend Architect

3. **Risk: PDF Export Memory Overhead**
   - **Impact:** Large reports (1000+ breach details, 100+ pages) cause PDF generation to consume 500MB+ heap per request, risking OOM
   - **Likelihood:** Low (most reports <20 pages), but possible during annual compliance audits
   - **Mitigation:** Implement async job queue with memory limits (max 200MB per job); paginate large reports (e.g., top 100 breaches only in PDF, full data in CSV); add alert on PDF job queue depth >50
   - **Owner:** Backend Lead + DevOps

4. **Risk: Report Query Performance Degradation**
   - **Impact:** Compliance reports slow to <5s after 2 years of breach log data (100k+ rows), frustrating managers
   - **Likelihood:** Medium (data volume grows linearly with tickets)
   - **Mitigation:** Partition `sla_breach_log` table by year (PostgreSQL declarative partitioning); add data retention policy (archive breaches >3 years to cold storage); monitor query latency and add indexes as needed
   - **Owner:** DBA + Backend Architect

**Assumptions:**

1. **Assumption:** Admins create <50 SLA policies (one policy map fits in Redis cache <100KB)
   - **Validation:** Most orgs have 5-10 SLA tiers (VIP, Critical, Standard, Low-priority)
   - **Impact if False:** Cache grows >1MB, consider LRU eviction or split cache by category

2. **Assumption:** Business hours are consistent across org (single timezone, single calendar)
   - **Validation:** Phase 1 targets single-tenant per environment
   - **Impact if False:** Add timezone field to policy, calculate SLA in requester's timezone (complex)

3. **Assumption:** Root cause tagging is manual or semi-automated (no ML model for auto-classification)
   - **Validation:** Phase 1 focuses on reporting infrastructure; ML deferred to Phase 3
   - **Impact if False:** N/A (enhancement opportunity)

4. **Assumption:** PDF export branding is basic (logo + header/footer; no custom CSS)
   - **Validation:** Stakeholders confirmed executive reports need minimal branding
   - **Impact if False:** Add CSS customization UI (complex, low ROI)

**Open Questions:**

1. **Q: Should policy changes apply retroactively to existing tickets or only new tickets?**
   - **Context:** Admin updates policy "Critical Network SLA" from 2h to 3h resolution; 50 open tickets already have 2h SLA
   - **Options:** (A) Retroactive (update SLA for all open tickets with this policy), (B) Prospective only (new tickets get 3h, old keep 2h)
   - **Decision Needed By:** Week 17 (Sprint 9) before policy versioning implementation
   - **Owner:** Product Manager (John) + Legal (compliance implications)

2. **Q: How to handle holiday calendars in business hours calculation?**
   - **Context:** Business hours exclude weekends, but not holidays (Christmas, Thanksgiving); SLA clock runs on holidays
   - **Options:** (A) Phase 1: No holiday support (manual SLA adjustment by admin), (B) Add holiday calendar config to policy
   - **Decision Needed By:** Week 17 (Sprint 9) before BusinessHoursCalculator implementation
   - **Owner:** Product Manager (John)

3. **Q: Should breach log store full ticket snapshots (title, description) or just IDs?**
   - **Context:** For breach analysis, helpful to see ticket details; but storing full snapshots bloats breach log table
   - **Options:** (A) Store ticket_id only (join to tickets table for details), (B) Denormalize title + category + priority (current design), (C) Store full JSON snapshot
   - **Decision Needed By:** Week 16 (Sprint 8) before breach log schema finalized
   - **Owner:** Backend Architect (storage vs query performance trade-off)

4. **Q: What is max PDF report page count (hard limit to prevent abuse)?**
   - **Context:** Prevent user from generating 10,000-page PDF that takes 1 hour and crashes server
   - **Options:** (A) Hard limit 100 pages (reject larger reports with 400 error), (B) Hard limit 1000 pages with async only, (C) No limit (trust async job timeout)
   - **Decision Needed By:** Week 18 (Sprint 9) before PDF export implementation
   - **Owner:** Backend Lead + Product Manager

## Test Strategy Summary

**Unit Tests (JUnit 5 + Mockito):**
- **SlaPolicyEvaluator:** Test condition matching logic (category=Network AND priority=Critical matches ticket); cache hit/miss scenarios
- **BusinessHoursCalculator:** Extensive edge case tests (weekend skip, Friday 4:50pm + 30min SLA, multi-day SLA spanning weekend, DST transition)
- **ReportingService:** Test aggregation logic (compliance rate calculation, trend data grouping by date, root cause distribution)
- **JSON Schema Validator:** Test policy rules validation (valid/invalid JSON, missing required fields)
- **Coverage Target:** >80% for Reporting Module, SlaPolicyEvaluator, BusinessHoursCalculator

**Integration Tests (Spring Boot Test + Testcontainers):**
- **Policy CRUD:** Create/update/delete policy → assert DB rows, versioning table, cache invalidation
- **Policy Evaluation:** Create ticket with matching policy → assert policy_id stored, SLA due_at calculated correctly
- **Breach Logging:** Emit TicketSlaBreached event → assert breach log row created with root cause tag
- **Report Generation:** Seed 100 tickets + 8 breaches → query compliance report → assert 92% rate, chart data correct
- **PDF Export:** Generate small report → export PDF → assert PDF valid (open in reader), latency <5s

**E2E Tests (Playwright):**
- **Happy Path:** Admin creates policy → agent creates matching ticket → manager views compliance report showing ticket's SLA compliance
- **Policy Editor:** Admin builds complex rule (3 conditions) via UI drag-drop → saves → verify JSON structure correct
- **PDF Download:** Manager generates breach analysis → clicks Export PDF → verify download starts within 5s

**Performance Tests (Gatling):**
- **Policy Evaluation Load:** Simulate 100 concurrent ticket creations → measure cache hit rate >95%, p95 latency <100ms
- **Report Query Baseline:** Query compliance report for 6 months (1000 tickets) → p95 <1s
- **Report Query Stress:** Query 2 years of data (20k tickets) → verify p95 <2s (with partitioned table)
- **PDF Export Concurrency:** 10 concurrent PDF exports → verify queue depth stable, no OOM

**Security Tests:**
- **RBAC:** Test Agent calling Admin-only policy editor → assert 403
- **RBAC:** Test Agent querying other agent's performance → assert 403
- **RBAC:** Test Manager viewing all reports → assert 200 OK
- **JSON Schema Injection:** Attempt policy with malicious JSON (`{"$ref": "file:///etc/passwd"}`) → assert validation fails

**Data Migration Tests (if upgrading from Epic 2 to Epic 6):**
- **Backward Compatibility:** Existing tickets from Epic 2 (no active_policy_id) continue to work with hardcoded SLA
- **Migration Script:** Test migration adding `active_policy_id` column → populate with "Default Policy" for existing tickets → assert no data loss

**Acceptance Testing (UAT):**
- **Business Hours Validation:** Real users create tickets at various times (Friday evening, Monday morning, mid-week) → verify SLA due dates match expectations
- **Report Usability:** Managers navigate compliance dashboard, drill down by category, export PDF → gather feedback on chart clarity, load times
- **Policy Editor Usability:** Admins create 5 real-world policies (VIP SLA, Critical Infrastructure, Standard, Low-priority, After-hours) → verify UI supports use cases

**Rollout Plan (if Epic 6 approved):**
- **Week 16-17 (Sprint 8):** Backend implementation (policy CRUD, evaluation, breach logging)
- **Week 18 (Sprint 9):** Frontend implementation (policy editor UI, report dashboards)
- **Week 19 (Sprint 10):** PDF export, scheduled emails, UAT
- **Week 20 (Sprint 10):** Bug fixes, performance tuning, production deployment
- **Post-Launch:** Monitor policy cache hit rate, report query latency, PDF export queue depth; iterate based on usage patterns
