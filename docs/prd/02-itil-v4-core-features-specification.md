---
id: 02-itil-v4-core-features-specification
title: 2. ITIL v4 Core Features Specification
version: 8.0
last_updated: 2025-09-03
owner: Product Manager
status: Draft
---

## 2. ITIL v4 Core Features Specification

### 2.1 Incident Management
Incident Management: A sophisticated system for logging, tracking, and resolving IT incidents.

Key capabilities:
- Automated ticket routing based on technician availability and expertise
- Multi-channel ticket creation: email, self-service portal, mobile app
- Customizable incident templates and categories
- SLA management with automated escalations for potential breaches

Acceptance Criteria:
- Tickets can be created via email, portal, and mobile app with attachments
- Auto-routing assigns to an available technician matching skill tags within SLA thresholds (e.g., <5 minutes)
- SLA timers start at creation; pre-breach alerts trigger at configurable thresholds (e.g., 80%)
- Incident templates configurable per category/team; applied at ticket creation
- All status transitions are audit logged with user, timestamp, and notes


### 2.2 Problem Management
Problem Management: Proactively identify and eliminate the root causes of recurring incidents.

Key capabilities:
- In-depth problem analysis and correlation of related incidents
- Known error record creation and lifecycle management
- Association of incidents to a single problem ticket for efficient resolution

Acceptance Criteria:
- Create problem records from one or more incidents; maintain bidirectional links
- Create/search Known Error records and attach workarounds to incidents
- RCA fields (cause, contributing factors, corrective/preventive actions) are required at closure
- Trend reports identify top recurring categories/services over configurable periods
- Problem lifecycle enforces statuses and approvals (e.g., Draft → Analysis → Resolved → Closed)

### 2.3 Change Management
Change Management: A structured approach to managing changes in the IT infrastructure to minimize disruptions.

Key capabilities:
- Visual workflow builder for change processes (submission → approval → implementation → review)
- Change calendar to prevent scheduling conflicts
- Standard change templates, risk assessment, and CAB approvals

Acceptance Criteria:
- Visual workflow builder supports steps, conditions, and approvals; workflows are versioned
- Calendar prevents conflicts (blackout windows, CI conflicts) with warnings and blocks as configured
- Approval gating enforces CAB sign-off by risk level; all decisions audit logged
- Mandatory rollback plan for medium/high-risk changes prior to approval
- Notifications sent to stakeholders on status changes (submitted, approved, scheduled, implemented)

### 2.4 Release Management
Release Management: Plan, build, test, and deploy releases effectively.

Key capabilities:
- Create release workflows and gates
- Associate changes and problems to a release
- Track the end-to-end release process from development to deployment

Acceptance Criteria:
- Releases can link related change/problem/incident records
- Stage gates enforce readiness (tests passed, approvals complete) before promotion
- Track deployments across environments with current status and timestamps
- Rollback instructions are stored and validated prior to go-live
- Release success rate and MTTR are reported per period

### 2.5 Self-Service Portal
Self-Service Portal: A user-friendly, customizable portal that empowers end-users to log tickets, track status, search the knowledge base, and browse the service catalog.

Key capabilities:
- Guided ticket submission with dynamic forms and suggestions
- Ticket status tracking with notifications
- Integrated knowledge search and service catalog browsing
- Reduces help desk workload for common requests

Acceptance Criteria:
- End users can log and track tickets; view history and communications
- Knowledge base search with previews; agents can attach KB to tickets; users rate usefulness
- Service catalog browsing with filtering; requests pre-populate forms and show SLAs
- Branding (logo/colors) and localization configurable; WCAG 2.1 AA compliant
- SSO login supported; user preferences (theme, notifications) persisted
- Common request templates (password reset, access requests) available with guided flows

### 2.6 Service Catalog
Service Catalog: A comprehensive catalog of all available IT and business services that users can request.

Key capabilities:
- Service offering definitions with categories and SLAs
- Custom fulfillment workflows and multi-stage approvals
- Request forms with dynamic fields and validation

Acceptance Criteria:
- Create/edit/deactivate services with owner, category, SLA, and form schema
- Dynamic forms support conditional fields, validation, and file attachments
- Per-service approval workflows (single/multi-stage) configurable
- Fulfillment workflows execute tasks with audit trail and notifications
- Service performance metrics (volume, fulfillment time, CSAT) available
- Automated fulfillment for standard requests
- Multi-stage approval workflows support delegation

### 2.7 Knowledge Base
Knowledge Base: A centralized repository of articles, FAQs, and solutions to promote faster resolution and self‑help.

Key capabilities:
- Rich text articles with attachments and versioning
- Approval workflows for new and updated content
- Relevance‑ranked search and related article suggestions

Acceptance Criteria:
- Article workflow enforces Draft → Review → Publish with role-based approvals
- Version history retained with ability to rollback; attachments stored securely
- Full-text search returns results with relevance ranking under 800ms (p95)
- Related articles suggested during ticket creation and update
- Expiry notifications sent to content owners for review/renewal

### 2.8 IT Asset Management (ITAM)
IT Asset Management (ITAM): Discover and manage hardware and software assets across the network.

Key capabilities:
- Automated discovery and detailed inventory (ownership, status, lifecycle)
- Software license compliance tracking and reporting
- Barcode/QR code scanning for simplified asset tracking

Acceptance Criteria:
- Discovery populates inventory; manual import/export supported (CSV)
- Asset ownership, location, and lifecycle statuses tracked with history
- License usage vs entitlements monitored; alerts on non-compliance
- Barcode/QR scanning updates asset status/location via mobile
- Incidents/changes can link to assets; impact reports include asset context



### 2.9 Configuration Management Database (CMDB)
CMDB: A centralized repository to store information about all configuration items (CIs) and their relationships.

Key capabilities:
- CI lifecycle tracking with relationships and dependencies
- Impact analysis for incidents, problems, and changes
- Supports faster troubleshooting and better risk assessment

Acceptance Criteria:
- Create and maintain CIs with type-specific attributes and owners
- Relationship graph visualized; upstream/downstream traversal for impact
- Impact analysis available before approving changes and during incidents
- Baseline and snapshot management with diff view
- CMDB provides event-driven impact assessments to the Change module for risk/impact checks

### 2.10 Customizable Dashboards
Customizable Dashboards: Real-time, interactive dashboards for key metrics, technician performance, and SLA compliance.

Key capabilities:
- Drag-and-drop widgets with role-based visibility
- Live data tiles for incidents, SLAs, backlog, and workloads
- Save/share dashboard layouts per user or team

Acceptance Criteria:
- Users create/edit personal dashboards; admins manage shared dashboards
- Widgets refresh automatically; configurable refresh intervals
- Role-based visibility per widget and dashboard
- Dashboards can be shared via link or assigned to teams/roles
- Export dashboard snapshots and widget data (CSV/PDF)

### 2.11 Pre-built and Custom Reports
Pre-built and Custom Reports: Out-of-the-box reports with a powerful builder for custom reporting, scheduling, and distribution.

Key capabilities:
- Library of common ITSM reports (SLA, backlog trends, MTTR)
- Report builder with filters, grouping, and charts
- Scheduling and automated distribution (email, export)

Acceptance Criteria:
- Pre-built reports are parameterized (date range, team, service)
- Custom report builder supports filters, grouping, aggregation, and charts
- Scheduled delivery (email) with per-report permissions
- Export to PDF, CSV, and XLSX; watermarks for sensitive reports
- Report execution logs retained with run history

### 2.12 Advanced Analytics Integration
Advanced Analytics Integration: Integrate with analytics platforms to derive insights, forecasts, and optimization opportunities.

Key capabilities:
- Connect to data platforms/warehouses for BI and ML
- Pre-modeled datasets for incidents, changes, assets, and SLAs
- Supports advanced analytics (e.g., trend analysis, anomaly detection)

Acceptance Criteria:
- Publish curated datasets for incidents/changes/assets/SLAs to the warehouse
- Validate BI connectivity (e.g., Power BI, Tableau) with role-based access
- Maintain data refresh schedules and lineage documentation
- Optional ML pipelines with monitoring for drift and model performance
- Governance: retention, PII handling, and access controls documented




## Review Checklist
- Content complete and consistent with PRD
- Acceptance criteria traceable to tests (where applicable)
- Data model references validated (where applicable)
- Event interactions documented (where applicable)
- Security/privacy considerations addressed
- Owner reviewed and status updated

## Traceability
- Features → Data Model: see 18-data-model-overview.md
- Features → Events: see 03-system-architecture-monolithic-approach.md
- Features → Security: see 13-security-compliance-framework.md
- Features → Tests: see 11-testing-strategy-spring-modulith.md
