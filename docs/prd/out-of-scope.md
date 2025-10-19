# Out of Scope

The following features are explicitly excluded from the initial product delivery to maintain focus on integrated ITSM+PM capabilities. SynergyFlow is built as a modular monolith with complete feature parity to ManageEngine ServiceDesk Plus, not as a replacement for specialized tools in these domains:

## Architecture Decisions (Permanently Out of Scope)

**1. Kafka Message Broker (Architectural Decision)**
- Kafka 3-broker cluster deployment with Strimzi operator
- Schema Registry (Confluent Community Edition) for Avro schemas
- External event publishing for third-party integrations
- Cross-system event streaming (monitoring tools, external APIs)
- **Rationale:** Spring Modulith in-JVM event bus is the permanent choice for modular monolith architecture (20x faster, eliminates 6 CPU cores, 13.5GB RAM, $250-350/month infrastructure costs)
- **Migration Path Available:** Spring Modulith event externalization enables future migration to Kafka if external event consumers become critical requirement (not currently planned)
- **Decision Documented:** Architecture.md Section 5 (Event-Driven Architecture), lines 1125-1545
- **Would Only Be Needed If:** External systems require consuming SynergyFlow events in real-time, or event throughput exceeds 10,000 events/second (not expected for target scale)

**2. Impact Orchestrator (Self-Optimizing Workflows)**
- Real-time signal fusion from SLOs, topology, and calendars
- Auto-tuning SLAs, routing, and priorities based on historical patterns
- Dynamic approval routing based on risk, impact, and capacity
- Task reprioritization based on business impact
- **Rationale:** Requires operational maturity and significant ML infrastructure; not essential for initial product delivery; can be evaluated after 12+ months operational data collection
- **Would Require:** Production deployment with 6+ months operational data, ML platform integration, dedicated data science resources

**3. Natural Language Control (NL→DSL Compiler)**
- Natural dialogue as primary interface for workflow commands
- NL→DSL compiler: "Auto-approve all low-risk changes to staging" → Rego policy
- Policy verification with safe execution sandbox
- Explainable execution with audit trails
- **Rationale:** Advanced AI/ML capability requiring LLM integration, prompt engineering, and extensive safety verification; not essential for initial product delivery
- **Would Require:** LLM partnership or self-hosted deployment, dedicated ML engineering resources, policy safety verification framework

**4. Mobile Native Apps**
- iOS native app (Swift, SwiftUI)
- Android native app (Kotlin, Jetpack Compose)
- Offline mode with sync
- Push notifications with device-specific targeting
- Mobile-specific workflows (barcode scanning for ITAM)
- **Rationale:** Progressive Web App (PWA) with responsive design sufficient for mobile access; native apps require dedicated mobile development team and ongoing maintenance
- **Would Require:** User validation of native app demand, dedicated iOS/Android developers, app store distribution infrastructure

**5. Multi-Tenancy Architecture**
- Tenant isolation (database-per-tenant or schema-per-tenant)
- Per-tenant customization (branding, workflows, policies)
- Tenant admin roles and delegated administration
- Cross-tenant analytics for SaaS provider
- Tenant provisioning automation
- **Rationale:** Product designed for single-tenant self-hosted deployment model; multi-tenancy adds significant architectural complexity and operational overhead not justified for target market
- **Would Require:** Fundamental architectural redesign, tenant management platform, billing integration, SaaS operational infrastructure

## Product Boundaries (Integration Points, Not Replacements)

**1. Full-Featured Project Management (Compete with JIRA)**
- Advanced agile features (burnup charts, cumulative flow diagrams, velocity forecasting)
- Portfolio management (program, initiative, portfolio levels)
- Advanced roadmapping (Gantt charts, critical path analysis)
- Resource capacity planning across projects
- **Rationale:** SynergyFlow provides PM basics for cross-module integration, not a full JIRA replacement

**2. Advanced ITOM (IT Operations Management)**
- Infrastructure monitoring and alerting (compete with DataDog, New Relic)
- Log aggregation and analysis
- APM (Application Performance Monitoring) with distributed tracing
- Synthetic monitoring and uptime checks
- **Rationale:** SynergyFlow integrates with existing ITOM tools via events, not replacing them

**3. Customer Service Management (CSM)**
- Customer-facing support portal (external customers, not internal IT)
- SLA tracking for external customers
- Customer satisfaction surveys (CSAT, NPS) for external customers
- Self-service knowledge base for external customers
- **Rationale:** SynergyFlow focuses on internal ITSM+PM, not external customer support

**4. Advanced DevOps Tooling**
- CI/CD pipeline orchestration (compete with Jenkins, GitLab CI)
- Infrastructure as Code (IaC) management
- Secret scanning and vulnerability management
- Container registry and artifact management
- **Rationale:** SynergyFlow integrates with existing DevOps tools, not replacing them

**5. Enterprise ERP Integration**
- Procurement and vendor management
- Financial management (budgeting, cost allocation)
- HR system integration (onboarding, offboarding)
- Facilities management
- **Rationale:** Out of scope for ITSM+PM platform, potential partnerships for integration

---
