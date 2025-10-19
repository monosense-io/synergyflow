# 4. ADVANCED CAPABILITIES PHASE EPICS (Months 8-11)

## Epic-07: IT Asset Management (ITAM)

**Goal:** Asset inventory, lifecycle tracking, license compliance.

**Value Proposition:** Prevent license violations, optimize asset utilization.

**Story Count:** 4 stories

**Key Capabilities:**
- Asset discovery pipelines with normalization
- Asset ownership and lifecycle tracking
- License monitoring with compliance alerts
- Mobile barcode/QR scanning for asset updates

**Dependencies:** CMDB (Epic-08) for CI linkage

---

## Epic-08: CMDB and Impact Assessment

**Goal:** Configuration Management Database with relationship graph and impact analysis.

**Value Proposition:** Enable "which changes affect this CI?" queries, reduce blast radius.

**Story Count:** 5 stories

**Key Capabilities:**
- CI model with types and relationships
- Relationship graph traversal APIs
- Baseline snapshot and diff management
- Impact assessment service integration with Change Management
- Data quality governance and validation

**Dependencies:** Change Management (Epic-02), Asset Management (Epic-07)

---

## Epic-09: Dashboards and Reports

**Goal:** Customizable dashboards and report builder.

**Value Proposition:** Real-time visibility into operational metrics, support data-driven decisions.

**Story Count:** 4 stories

**Key Capabilities:**
- Dashboard model with widget framework and drag-n-drop UI
- Live tiles with event stream updates
- Report builder with guardrails (pre-defined templates)
- Report scheduler with email delivery

**Dependencies:** Event System (Epic-00) for live updates

---

## Epic-10: Analytics and BI Integration

**Goal:** Dataset contracts for BI tools, ML pipeline templates.

**Value Proposition:** Enable advanced analytics, predictive incident prevention.

**Story Count:** 4 stories

**Key Capabilities:**
- Dataset export services with row-level security
- BI connectivity (Tableau, Power BI, Looker)
- Refresh scheduler with lineage tracker
- ML pipeline templates for prediction (incident spike, capacity planning)

**Dependencies:** Dashboards (Epic-09), Data platform

---

## Epic-11: Notifications and Communication

**Goal:** Multi-channel notification system with preferences and deduplication.

**Value Proposition:** Ensure users receive timely alerts without notification fatigue.

**Story Count:** 5 stories

**Key Capabilities:**
- Template engine with localization
- User preference subscriptions (email, in-app, push)
- Channel adapters (email, push, in-app notification center)
- Deduplication, grouping, digest mode
- Delivery metrics and audit reporting

**Dependencies:** Event System (Epic-00), User Management (FR-16)

---
