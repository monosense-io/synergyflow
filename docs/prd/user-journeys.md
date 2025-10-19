# User Journeys

## Journey 1: Incident Response and Cross-Module Resolution (Primary Persona: IT Service Desk Agent)

**Scenario:** A critical incident occurs affecting production services. Agent needs to investigate, resolve, and track associated change work.

**Steps:**

1. **Incident Alert Receipt**
   - Agent receives alert via email/push notification: "INC-1234: Production API returning 500 errors"
   - Clicks notification, redirects to SynergyFlow incident detail page
   - **Freshness Badge:** "Data current as of 1.2 seconds ago" (green) - builds trust in real-time data

2. **Incident Investigation**
   - Reviews incident details: Priority=Critical, Severity=S1, SLA=4 hours
   - **SLA Timer:** Flowable timer shows "3h 47m remaining" with yellow warning (approaching breach)
   - Checks related entities: **Link-on-Action** shows related recent deployment (CHANGE-567) via Work Graph
   - **Freshness Badge:** "Data current as of 0.05 seconds ago" (green) - in-memory event bus
   - Correlation discovered: incident spike correlates to deployment 2 hours ago

3. **Time Logging with Single-Entry**
   - Opens **Single-Entry Time Tray** from top navigation
   - Logs 30 minutes investigation time: "Analyzed logs, identified deployment correlation"
   - Time entry **automatically mirrors** to INC-1234 worklog and related task TASK-890
   - Agent sees confirmation: "Time logged to INC-1234, TASK-890"

4. **Creating Related Change for Rollback**
   - Clicks **"Create related change"** from incident detail page
   - SynergyFlow pre-fills change request:
     - Change Title: "Rollback deployment CHANGE-567 (incident INC-1234)"
     - Risk: High (auto-populated based on incident severity)
     - Impacted Services: API-Service (from incident context)
     - **Bidirectional link:** INC-1234 ↔ CHANGE-890 automatically established

5. **Policy-Driven Approval**
   - Submits change request CHANGE-890
   - **OPA Policy Evaluation:** Evaluates risk, impact, timing
   - **Decision Receipt:** "High-risk emergency change impacting critical service → CAB + CTO approval required (Policy v1.2.3)"
   - Change routed to CAB queue with full context
   - Agent sees transparent explanation: "Why does this need CAB approval? [View Decision Receipt]"

6. **Deployment Tracking and Resolution**
   - CAB approves change within 30 minutes (emergency process)
   - DevOps executes rollback deployment
   - **Event Flow:** ChangeDeployed event → incident auto-updated with deployment status
   - Agent verifies incident resolved, marks INC-1234 as Resolved
   - **Audit Trail:** Full history accessible showing incident → investigation → change → deployment → resolution

**Outcome:** Agent resolved critical incident in 1.5 hours (vs typical 4-6 hours), with full cross-module traceability and explainable automation.

---

## Journey 2: Project Planning with Operational Awareness (Secondary Persona: Engineering Manager)

**Scenario:** Engineering manager planning next sprint, needs to understand operational impact of recent releases.

**Steps:**

1. **Sprint Planning Preparation**
   - Opens SynergyFlow PM module, navigates to backlog
   - Reviews candidate stories for Sprint 12
   - Notices story STORY-340: "Deploy new payment gateway integration"

2. **Operational Impact Assessment**
   - **Work Graph Integration:** Checks "Related Incidents" panel
   - Sees recent deployment (last sprint's CHANGE-520) correlated with incident spike
   - **Freshness Badge:** "Incident data current as of 0.08 seconds ago" (green) - in-memory event bus
   - Identifies pattern: payment API changes frequently trigger incidents

3. **Risk Mitigation Planning**
   - Creates **related change request** from STORY-340 via Link-on-Action
   - Pre-fills change with deployment plan, rollback procedure
   - Adds "Extra QA cycle required" note based on incident history
   - Assigns story to Sprint 12 with adjusted story points (8 → 13, accounting for extra testing)

4. **Cross-Team Coordination**
   - **Link-on-Action:** Creates related task for DevOps: "Prepare blue-green deployment for payment gateway"
   - TASK-945 auto-linked to STORY-340 and CHANGE-600
   - DevOps sees task in their unified "My Work" view alongside incidents

5. **Release Monitoring**
   - After deployment, monitors change calendar
   - **Work Graph:** Automatically tracks incident correlation to CHANGE-600
   - No incident spike detected after 24 hours
   - Marks story STORY-340 as Done, closes change CHANGE-600 successfully

**Outcome:** Engineering manager identified risk early, coordinated cross-team work seamlessly, deployed safely with no incidents.

---

## Journey 3: Building Trust in Policy-Driven Automation (Primary Persona: Change Manager)

**Scenario:** Change manager transitioning from manual CAB approvals to policy-driven automation, needs to build trust incrementally.

**Steps:**

1. **Shadow Mode Testing**
   - New OPA policy created: "Auto-approve low-risk standard changes impacting <5 services"
   - Policy deployed in **Shadow Mode:** logs decisions without enforcing
   - Change manager reviews shadow mode logs for 2 weeks (100 changes evaluated)
   - **Decision Receipts:** Reviews "Would have auto-approved" decisions:
     - CHANGE-701: "Low-risk, 2 services, standard change → AUTO-APPROVE (Policy v2.0.0-shadow)"
     - CHANGE-715: "High-risk, 15 services → REQUIRES CAB (Policy v2.0.0-shadow)"

2. **Shadow Mode Validation**
   - Compares shadow mode decisions with actual CAB decisions
   - Agreement rate: 95% (95 out of 100 decisions match CAB)
   - 5 disagreements analyzed: Policy tuned to match CAB judgment
   - Policy updated to v2.1.0-shadow with refinements

3. **Canary Rollout**
   - Policy promoted to **Canary Mode:** enforces for 10% of traffic
   - Change manager monitors auto-approval outcomes for 1 week
   - **Metrics tracked:** Auto-approval rate, SLA compliance, incident correlation, rollback rate
   - Results: 80% auto-approval rate, 99% SLA compliance, zero incidents correlated to auto-approved changes

4. **Full Production Rollout**
   - Policy promoted to **Production Mode:** enforces for 100% of traffic
   - Change manager reviews weekly automation report
   - **Decision Receipt Coverage:** 100% of automated decisions have explainable receipts
   - **Audit Trail:** All decisions logged with policy version, input data, decision output

5. **Continuous Improvement**
   - Monthly policy review: analyze auto-approval outcomes, tune thresholds
   - Error budget monitoring: If SLA breaches exceed 5%, automation paused automatically
   - Team confidence established: "I trust the system because I can see exactly why every decision was made"

**Outcome:** Change manager successfully transitioned from 100% manual approvals to 80% automated approvals with full governance and explainability, reducing change lead time by 30% while maintaining zero incidents.

---

## Journey 4: End-User Self-Service Knowledge Search (Tertiary Persona: End User)

**Scenario:** End user experiencing VPN connection issue, uses self-service portal to find resolution before creating incident.

**Steps:**

1. **Knowledge Base Search**
   - User opens SynergyFlow portal, searches "VPN connection fails"
   - **Full-text search** returns 5 relevant articles ranked by relevance
   - Top result: "KB-0042: Troubleshooting VPN connection failures (v3.2)"
   - **Freshness Badge:** "Article reviewed 12 days ago" (green, within 30-day review cycle)

2. **Self-Service Resolution**
   - Follows troubleshooting steps from knowledge article
   - Step 3 resolves issue: "Clear VPN certificate cache"
   - User rates article: "Helpful" (thumbs up)
   - **Feedback Loop:** Rating increases article relevance score for future searches

3. **Proactive Notification (Avoided Incident)**
   - System tracks knowledge article usage: KB-0042 used 15 times this week
   - **Analytics:** Pattern detected, suggests creating known error record
   - IT team creates **Known Error** linked to KB-0042
   - Future VPN incidents auto-suggest KB-0042 resolution via self-healing engine

**Outcome:** End user resolved issue in 5 minutes without creating incident, IT team saved 30 minutes support time, knowledge base continuously improved via feedback loop.

---

## Journey 5: Operational Dashboard and Proactive SLA Management (Primary Persona: Incident Manager)

**Scenario:** Incident manager monitors team performance, proactively identifies SLA breach risks.

**Steps:**

1. **Dashboard Monitoring**
   - Opens SynergyFlow operations dashboard (custom configured with widgets)
   - **Live Tiles:** Real-time incident count, SLA compliance %, avg MTTR
   - **Freshness Badge:** "Data current as of 2.1 seconds ago" (green) - event stream updates

2. **SLA Breach Risk Identification**
   - Widget shows "3 incidents approaching SLA breach (< 30 minutes remaining)"
   - Drills down: INC-1340 (P2, 27 minutes remaining), INC-1341 (P1, 18 minutes remaining), INC-1342 (P2, 25 minutes remaining)
   - **Flowable SLA Timers:** Pre-breach notifications sent to assigned agents

3. **Proactive Intervention**
   - Reviews INC-1341 (highest priority, shortest remaining time)
   - Agent assigned: Alice (current workload: 5 open incidents)
   - **Routing Override:** Manually reassigns INC-1341 to Bob (current workload: 2 open incidents)
   - Bob receives notification: "INC-1341 reassigned to you (SLA breach in 18 minutes)"

4. **Performance Analytics**
   - Reviews weekly MTTR trend: ↓25% over last 4 weeks (from 6.2 hours → 4.7 hours)
   - Identifies driver: Single-Entry Time Tray adoption (85% of agents using weekly)
   - **Business Metrics Dashboard:** Links productivity gain to feature adoption

**Outcome:** Incident manager prevented SLA breaches through proactive monitoring, demonstrated quantifiable productivity gains from platform adoption.
