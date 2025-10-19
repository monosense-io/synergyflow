# 📥 Linear Import Guide — SynergyFlow Sprint Plan

> **Complete guide to import 24-sprint backlog into Linear** | CSV + API + Manual options

---

## 📍 Table of Contents

- [Quick Start](#quick-start)
- [Method 1: CSV Import (Easiest)](#method-1-csv-import-easiest)
- [Method 2: Linear API (Fastest for Bulk)](#method-2-linear-api-fastest-for-bulk)
- [Method 3: Manual Import via UI](#method-3-manual-import-via-ui)
- [Linear Schema Mapping](#linear-schema-mapping)
- [CSV Files Ready to Import](#csv-files-ready-to-import)
- [Linear API Payload (JSON)](#linear-api-payload-json)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

### **Fastest Path (5 minutes):**
1. Copy CSV from [CSV Files Ready to Import](#csv-files-ready-to-import)
2. Go to Linear workspace → Settings → Import
3. Select "CSV" → Upload file
4. Map columns (usually auto-detected)
5. Click "Import" → Done ✅

### **For Advanced Users (API):**
1. Get Linear API key from workspace settings
2. Copy JSON from [Linear API Payload (JSON)](#linear-api-payload-json)
3. Run script with `curl` or Postman
4. All issues created in seconds ✅

### **Manual Path (30 minutes):**
Use swimlane cards + Lane-compatible templates to create issues one-by-one in Linear UI

---

## Method 1: CSV Import (Easiest)

### Step 1: Prepare CSV File

Copy the CSV from section [CSV Export Format](#csv-export-format-ready-to-use) and save as `synergyflow-sprints.csv`

### Step 2: Access Linear Import

```
1. Go to Linear workspace
2. Click Settings (⚙️) → Integrations → Import
   OR
   Go directly to: https://linear.app/[workspace]/settings/integrations/import
3. Click "Import" → Select "CSV"
```

### Step 3: Upload & Map

```
1. Click "Choose File" → Select synergyflow-sprints.csv
2. Linear auto-detects columns; verify mapping:
   ✅ Title → Title
   ✅ Description → Description
   ✅ Status → Status (Backlog, Todo, In Progress, Done)
   ✅ Priority → Priority (0=No priority, 1=Urgent, 2=High, 3=Medium, 4=Low)
   ✅ Assignee → Assignee
   ✅ Estimate → Estimate (story points)
   ✅ Team → Team (backend, frontend, devops, qa)
   ✅ Cycle → Cycle (Sprint 01, Sprint 02, etc.)
   ✅ Labels → Labels (comma-separated tags)
3. Click "Next"
```

### Step 4: Review & Import

```
1. Preview shows number of issues to import (should be 120+)
2. Click "Import" → Linear processes CSV
3. Wait for completion (may take 1-5 minutes for large backlog)
4. ✅ All issues created!
```

### Step 5: Verify Import

```
After import completes:
1. Go to Issues view
2. Filter by Team or Cycle
3. Verify story counts:
   • Total stories: ~120
   • Phase 1 (Sprints 01-04): 12 stories
   • Phase 2 (Sprints 05-12): 48 stories
   • Phase 3 (Sprints 13-22): 50 stories
   • Phase 4 (Sprints 23-24): 10 stories
```

---

## Method 2: Linear API (Fastest for Bulk)

### Step 1: Get Linear API Key

```
1. Go to Linear workspace → Settings (⚙️)
2. Click "API" or "Personal API Keys"
3. Click "Create new" API key
4. Copy the key (starts with "lin_")
5. Keep it secure (don't commit to git)
```

### Step 2: Prepare API Script

Create file `linear-import.sh`:

```bash
#!/bin/bash

API_KEY="lin_YOUR_API_KEY_HERE"
WORKSPACE_ID="YOUR_WORKSPACE_ID_HERE"
TEAM_ID="YOUR_TEAM_ID_HERE"

# Function to create issue
create_issue() {
    local title=$1
    local description=$2
    local priority=$3
    local estimate=$4
    local labels=$5

    curl -X POST "https://api.linear.app/graphql" \
        -H "Authorization: Bearer $API_KEY" \
        -H "Content-Type: application/json" \
        -d "{
            \"query\": \"mutation { issueCreate(input: { teamId: \\\"$TEAM_ID\\\", title: \\\"$title\\\", description: \\\"$description\\\", priority: $priority, estimate: $estimate, labelIds: [$labels] }) { issue { id title } } }\"
        }"
}

# Example: Create Sprint 01 stories
create_issue \
    "Sprint 01: Platform Foundation Kickoff" \
    "Phase 1: Weeks 1-2 | Epic-16 Platform Foundation" \
    2 \
    "0" \
    ""

echo "✅ Issue creation initiated"
```

### Step 3: Run Script

```bash
export API_KEY="lin_YOUR_API_KEY"
export WORKSPACE_ID="your-workspace-id"
export TEAM_ID="your-team-id"

bash linear-import.sh
```

### Step 4: Verify in Linear UI

```
Issues created automatically in Linear
✅ Appears in Issues → All Issues view
✅ Organized by Team (if team_id mapped correctly)
```

---

## Method 3: Manual Import via UI

### Batch Create via Linear UI

```
1. Go to Linear workspace → Issues
2. Click "+" → "New issue"
3. Fill in:
   • Title: [Story ID] [Title]
   • Description: [Acceptance criteria]
   • Team: [Backend/Frontend/DevOps/QA]
   • Cycle: [Sprint NN]
   • Priority: [0-3]
   • Estimate: [5/8/13/21]
   • Assignee: [Team member]
4. Click "Create" → Repeat for each story
```

**Time estimate:** ~30 seconds per story × 120 stories = ~60 minutes

**Tip:** Use keyboard shortcut `Cmd+K` then `+` to quickly create new issues

---

## Linear Schema Mapping

### CSV Column Mapping

| CSV Column | Linear Field | Type | Example |
|------------|--------------|------|---------|
| `Title` | Title | Text | "Sprint 01: Platform Foundation Kickoff" |
| `Description` | Description | Text | "Phase 1: Weeks 1-2. Main Epic: Epic-16..." |
| `Identifier` | ID (auto) | Auto | AUTO-1, AUTO-2, etc. |
| `Status` | State | Enum | "Backlog" \| "Todo" \| "In Progress" \| "Done" |
| `Priority` | Priority | Enum | 0=None, 1=Urgent, 2=High, 3=Medium, 4=Low |
| `Assignee` | Assignee | User | "@backend-1", "@frontend", "@devops" |
| `Estimate` | Estimate | Number | 5, 8, 13, 21 (story points) |
| `Team` | Team | Enum | "backend", "frontend", "devops", "qa" |
| `Cycle` | Cycle | Text | "Sprint 01", "Sprint 02", ... "Sprint 24" |
| `Labels` | Labels | Array | "phase-1,critical-path" |
| `Parent` | Parent Issue | ID | "AUTO-1" (if sub-task) |
| `Dependency` | Relationships | Array | "Blocked by AUTO-5" |

### Linear Priority Mapping

```
Linear Priority    Urgency
═══════════════════════════════════════════
0 (None)           Not started / Planning
1 (Urgent)         🔴 Critical path (MUST complete)
2 (High)           🟡 Blocks other work
3 (Medium)         🟢 Standard sprint work
4 (Low)            Future consideration
```

**SynergyFlow Mapping:**
- **Epic-16, Epic-00, Epic-01/02 early stories:** Priority 1 (Critical Path)
- **Core features (Epic-03/04):** Priority 2 (High)
- **Advanced capabilities:** Priority 3 (Medium)
- **Hardening (Epic-12-15):** Priority 2 (High)

---

## CSV Files Ready to Import

### CSV Export Format (Ready to Use)

```csv
Title,Description,Status,Priority,Assignee,Estimate,Team,Cycle,Labels,Dependency
"Sprint 01: Platform Foundation Kickoff","Phase 1 (Weeks 1-2) | Epic-16: Platform Foundation | Goals: Modulith baseline, DB, CI/CD skeleton | Exit: Modulith online, DB reachable, CI green","Backlog",1,"@backend-1",0,"backend","Sprint 01","phase-1,platform","None"
"16.05: Spring Modulith Configuration","Modulith config with event registry and boundaries. AC: ApplicationModules#verify() passes; Event publish/consume working; No cross-module deps; Audit log tracks changes","Todo",1,"@backend-1",13,"backend","Sprint 01","epic-16,critical-path","None"
"16.10: PostgreSQL + PgBouncer Setup","Database cluster with connection pooling. AC: PgBouncer running; Schemas created; JDBC pool 20-100; Hikari config; Health check passes","Todo",1,"@devops",13,"devops","Sprint 01","epic-16,critical-path","None"
"16.14: CI/CD Workflows Skeleton","GitHub Actions pipeline with build gates. AC: Actions runs on push; Build passes; Unit tests gated; Manual promotion gates for Stg/Prod","Todo",1,"@devops",8,"devops","Sprint 01","epic-16,critical-path","Blocked by 16.10"
"Reserve: Next.js Bootstrap Verification","Verify Next.js app boot (already done). AC: App runs locally; Tailwind loaded; TypeScript strict mode passes","Backlog",3,"@frontend",3,"frontend","Sprint 01","supporting","None"
"16.11: DragonflyDB Cache Configuration","Cache cluster setup with pooling. AC: Cache abstraction layer; Dragonfly connections stable; TTL policies; Metrics tracked; <5ms lookups p99","Todo",2,"@backend-2",13,"backend","Sprint 02","epic-16,performance","Blocked by 16.05"
"16.12: Observability Stack (Victoria Metrics + Grafana)","Metrics + dashboards setup. AC: Victoria Metrics running; Grafana dashboards for latency/errors/lag; OpenTelemetry configured; Alerts defined","Todo",1,"@devops",13,"devops","Sprint 02","epic-16,critical-path,observability","Blocked by 16.10"
"16.13: GitOps Deployment (Flux CD)","Infrastructure-as-Code with GitOps. AC: Flux CD bootstrap; Dev overlay deploys from main; Secrets via SOPS; IaC in git; Auto-reconciliation enabled","Todo",1,"@devops",13,"devops","Sprint 02","epic-16,critical-path,infrastructure","Blocked by 16.10"
"Support: Telemetry Integration","API client telemetry and error boundaries. AC: API client logs requests/responses; Error boundaries render; Trace IDs propagated; No console errors","Backlog",2,"@frontend",5,"frontend","Sprint 02","supporting","None"
"00.01: Event System + Transactional Outbox","Core event infrastructure with durability. AC: Outbox table created; Poller drains events; p95 <200ms; Idempotency key; Flyway migration; No duplicates","Todo",1,"@backend-1",21,"backend","Sprint 03","epic-00,critical-path,events","Blocked by 16.05"
"00.03: Link Infrastructure (Backend)","Bidirectional link persistence. AC: Link entity created; Cascades safe; Cross-module queries work; Deletion cascades safe; Audit trail immutable","Todo",2,"@backend-2",13,"backend","Sprint 03","epic-00,links","None"
"00.03: Link-on-Action UX (Frontend)","Link creation affordances. AC: Link affordance visible in context menu; Modal for selecting targets; Toast feedback; Bidirectional visible","Todo",2,"@frontend",8,"frontend","Sprint 03","epic-00,links,ux","Blocked by 00.03_BE"
"OPA: OPA Sidecar Setup (Shadow Mode)","OPA infrastructure setup. AC: OPA sidecar running; Policies injected; Logs captured; Correlation IDs; <50ms latency p99; Shadow mode logs decisions","Todo",2,"@devops",13,"devops","Sprint 03","epic-00,security,infrastructure","Blocked by 16.12"
"Contracts: Event Schema AsyncAPI","Event schema contracts. AC: AsyncAPI spec for all event types; Examples valid; Compatibility tests; CI integration; Contract tests passing","Backlog",2,"@qa",8,"qa","Sprint 03","testing,contracts","None"
"Contracts: Link Endpoint OpenAPI","Link API contracts. AC: OpenAPI spec for POST /api/links; Payload examples valid; Error responses documented; Contract tests in CI","Backlog",2,"@qa",5,"qa","Sprint 03","testing,contracts","None"
"00.02: Time Tray MVP","Work-on-entry mirroring. AC: Mirror worklogs from creation; Time Tray API returns mirrors; Refresh handles deletes; Timestamps accurate; Concurrent updates safe","Todo",2,"@backend-1",21,"backend","Sprint 04","epic-00,trust-ux","Blocked by 00.01"
"00.05: Decision Receipts (Backend)","Policy decision audit trail. AC: Receipt schema created; 100% policy evals logged; Receipts queryable; Immutable (no deletes); Audit trail only","Todo",2,"@backend-3",13,"backend","Sprint 04","epic-00,security,audit","Blocked by OPA"
"00.04: Freshness Badges","Staleness UI indicator. AC: Badge renders on detail; Colors: green <1h, yellow 1-4h, red >4h; Hover shows timestamp; Configurable thresholds","Todo",2,"@frontend",8,"frontend","Sprint 04","epic-00,trust-ux,ui","None"
"00.05: Policy Studio UI (Frontend)","Policy decision viewer. AC: Shadow mode toggle visible; Receipt viewer shows policy+evidence; Queryable history; CSV export supported","Todo",2,"@frontend",13,"frontend","Sprint 04","epic-00,security,ui","Blocked by 00.05_BE"
"OPA: Production Rollout Patterns","OPA deployment automation. AC: Policies deployed via CI/CD; Git versioning; Rollback automation; <30s update latency; No traffic loss during updates","Todo",2,"@devops",13,"devops","Sprint 04","epic-00,security,infrastructure","Blocked by OPA_shadow"
"E2E: Time Tray End-to-End Flow","Time Tray workflow validation. AC: Create incident > mirror appears; Modify entry > mirror updates; Concurrent updates are safe (no races)","Backlog",2,"@qa",8,"qa","Sprint 04","testing,e2e","Blocked by 00.02"
"E2E: Policy Receipt Validation","Receipt immutability validation. AC: Policy eval > receipt created; Receipt immutable (no updates); Queryable by entity; Audit trail complete","Backlog",2,"@qa",5,"qa","Sprint 04","testing,e2e,audit","Blocked by 00.05_BE"
"Sprint 05: Incident Management (Part 1)","Phase 2 (Weeks 9-10) | Epic-01: Incident Management | Goals: Incident CRUD, SLA timers, comments/attachments | Exit: Workflows demoable, timers ±5s","Backlog",2,"@backend-1",0,"backend","Sprint 05","phase-2,incidents","Blocked by 00.01"
"01.01: Incident CRUD + Classification","Incident domain with CRUD API. AC: Incident entity (title, desc, priority, severity); CRUD API functional; Validation enforced; Audit log tracked","Todo",2,"@backend-1",21,"backend","Sprint 05","epic-01,incidents,crud","Blocked by 00.01"
"01.03: SLA Timer Tracking (Flowable)","SLA timer infrastructure with Flowable. AC: SLA thresholds per priority; Flowable processes start on creation; Timers ±5s accurate (p95); Pre-breach notifications at 80%","Todo",2,"@backend-2",21,"backend","Sprint 05","epic-01,incidents,sla","Blocked by 16.12"
"01.04: Incident Lifecycle Workflow","Incident state machine. AC: States Open→Assigned→InProgress→Resolved→Closed; Transitions validated; Events published; Role permissions enforced","Todo",2,"@backend-3",13,"backend","Sprint 05","epic-01,incidents,workflow","Blocked by 01.01"
"01.UI: Incident List + Detail Pages","Incident UI components. AC: List page with sorting/filtering; Detail page with state buttons; Comments section stub; Responsive design; Mobile-friendly","Todo",2,"@frontend",8,"frontend","Sprint 05","epic-01,incidents,ui","Blocked by 01.01"
"01.Alerts: SLA Alerting Setup","Grafana + Slack alerting. AC: Alert rule for >80% SLA elapsed; Routes to Slack; Pre-breach 30 min before deadline; No alert spam (dedupe)","Todo",2,"@devops",13,"devops","Sprint 05","epic-01,observability,alerts","Blocked by 16.12"
"01.Storage: Attachment Storage Infrastructure","S3 bucket for attachments. AC: S3 bucket created (dev/stg/prod); 50MB file limit; Access control per owner+assignee; Virus scan ready","Todo",2,"@devops",8,"devops","Sprint 05","epic-01,infrastructure,storage","Blocked by 16.10"
"WebMvc: Incident CRUD Unit Tests","CRUD unit test coverage. AC: Create/Update/Delete tests; State transitions validated; Cascade behavior tested; >80% coverage","Backlog",2,"@qa",8,"qa","Sprint 05","testing,unit-tests","Blocked by 01.01"
"E2E: Happy Path - Create > Assign","Incident E2E happy path. AC: Create incident via UI; Search for incident; Assign to team member; SLA timer visible; Appears in list","Backlog",2,"@qa",8,"qa","Sprint 05","testing,e2e","Blocked by 01.01"
```

### How to Use CSV

1. **Copy entire CSV above** (starting from first line `Title,Description...`)
2. **Save to file:** `synergyflow-sprints-phase1.csv`
3. **Import to Linear:** Settings → Integrations → Import → CSV → Upload file
4. **Map columns** (usually auto-detected)
5. **Click Import** → Done ✅

---

## Linear API Payload (JSON)

### GraphQL Mutation for Bulk Create

If using Linear API directly (advanced):

```graphql
mutation CreateIssues {
  issueCreate(input: {
    teamId: "YOUR_TEAM_ID",
    title: "Sprint 01: Platform Foundation Kickoff",
    description: "Phase 1 (Weeks 1-2) | Epic-16: Platform Foundation | Goals: Modulith baseline, DB, CI/CD skeleton",
    priority: 1,
    estimate: 0,
    labelIds: ["PHASE-1", "PLATFORM"]
  }) {
    issue {
      id
      title
      identifier
    }
  }
}
```

### JavaScript Bulk Create Script

Save as `linear-bulk-import.js`:

```javascript
const LINEAR_API_KEY = "lin_YOUR_API_KEY_HERE";
const TEAM_ID = "YOUR_TEAM_ID_HERE";

const stories = [
  {
    title: "16.05: Spring Modulith Configuration",
    description: "Modulith config with event registry...",
    priority: 1,
    estimate: 13,
    labels: ["epic-16", "critical-path"]
  },
  {
    title: "16.10: PostgreSQL + PgBouncer Setup",
    description: "Database cluster with connection pooling...",
    priority: 1,
    estimate: 13,
    labels: ["epic-16", "critical-path"]
  },
  // ... add more stories
];

async function createIssue(story) {
  const query = `
    mutation {
      issueCreate(input: {
        teamId: "${TEAM_ID}",
        title: "${story.title.replace(/"/g, '\\"')}",
        description: "${story.description.replace(/"/g, '\\"')}",
        priority: ${story.priority},
        estimate: ${story.estimate},
        labelIds: ${JSON.stringify(story.labels)}
      }) {
        issue {
          id
          identifier
          title
        }
      }
    }
  `;

  const response = await fetch("https://api.linear.app/graphql", {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${LINEAR_API_KEY}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ query }),
  });

  const result = await response.json();
  return result.data.issueCreate.issue;
}

async function bulkImport() {
  console.log("Starting bulk import...");
  for (const story of stories) {
    const issue = await createIssue(story);
    console.log(`✅ Created: ${issue.identifier} - ${issue.title}`);
  }
  console.log("✅ All stories imported!");
}

bulkImport().catch(console.error);
```

### Run Script

```bash
node linear-bulk-import.js
```

---

## Troubleshooting

### Q: CSV Import Says "No Columns Match"

**A:** Column names must be exact. Verify header row:
```
✅ Correct: Title, Description, Status, Priority
❌ Wrong: title, description, status, priority (case-sensitive)
```

### Q: Issues Created But No Team Assignment

**A:** Team column must match exact team name in Linear. Check:
```
Go to Linear Settings → Teams → Copy exact team name
Use in CSV Team column: "Backend" vs "backend" vs "Engineering/Backend"
```

### Q: API Key Invalid Error

**A:** Regenerate API key:
```
1. Go to Linear Settings → API or Personal API Keys
2. Delete old key (if needed)
3. Create new key
4. Copy full key (starts with "lin_")
5. Test: curl -H "Authorization: Bearer lin_YOUR_KEY" https://api.linear.app/graphql
```

### Q: Assignee Not Set on Import

**A:** Assignee must be exact email or username. Verify in Linear:
```
Settings → Workspace Members
Copy exact email/username from list
Use in CSV: @backend-1 must match actual username
```

### Q: Import Takes Too Long

**A:** Linear processes in batches. For 120+ stories, may take 5-10 minutes. Do not refresh page. Check import status in Settings → Import History.

### Q: Cycle (Sprint) Not Assigned

**A:** Cycles must exist before import. Create cycles first:
```
1. Go to Linear → Cycles (left sidebar)
2. Click "Create cycle" → Create Sprint 01, Sprint 02, etc.
3. Then import CSV (Cycle column will match)
```

### Q: Duplicate Issues Created

**A:** Linear prevents duplicates by ID. If import fails midway and retries, may create duplicates. Solution:
```
1. Go to Issues
2. Search for duplicates
3. Delete extras manually
4. Re-run import (will skip existing by ID)
```

---

## Post-Import Checklist

After importing to Linear:

- [ ] **Verify counts:** 120+ issues created in correct cycles
- [ ] **Check teams:** All issues assigned to correct team (Backend/Frontend/DevOps/QA)
- [ ] **Verify priorities:** Critical path stories marked Priority 1 (Urgent)
- [ ] **Confirm estimates:** All stories have story points (5/8/13/21)
- [ ] **Check cycles:** Issues organized into Sprint 01–24
- [ ] **Test relationships:** Some issues marked as "Blocked by" (dependencies)
- [ ] **Validate labels:** Phase labels (phase-1, phase-2, etc.) applied
- [ ] **Set up views:** Create custom views for swimlanes (Backend/Frontend/DevOps/QA)
- [ ] **Configure notifications:** Set up team notifications for cycle starts

---

## Linear Views Setup (After Import)

### Create Swimlane Views

In Linear, create custom views for each team:

**View: "Backend Lane"**
```
Filter: Team = Backend
Sort By: Priority (High → Low), then Estimate (Large → Small)
Group By: Cycle (Sprint 01, Sprint 02, ...)
```

**View: "Frontend Lane"**
```
Filter: Team = Frontend
Sort By: Priority, then Estimate
Group By: Cycle
```

**View: "DevOps Lane"**
```
Filter: Team = DevOps
Sort By: Priority, then Estimate
Group By: Cycle
```

**View: "QA Lane"**
```
Filter: Team = QA
Sort By: Priority, then Estimate
Group By: Cycle
```

### Create Phase Views

**View: "Phase 1 (Foundation)"**
```
Filter: Labels includes "phase-1"
Sort By: Cycle, then Priority
Group By: Team
```

**View: "Critical Path"**
```
Filter: Labels includes "critical-path"
Sort By: Priority, then Estimate
Group By: Cycle
```

---

## Summary

| Method | Time | Pros | Cons |
|--------|------|------|------|
| **CSV Import** | 5 min | Fast, easy, no coding | Manual column mapping |
| **Linear API** | 10 min | Bulk + automated | Requires API key + script |
| **Manual UI** | 60 min | Full control, visual | Slow for 120+ stories |

**Recommendation:** Start with **CSV Import** (Method 1) for simplicity. If you have many teams, consider **API** (Method 2) for speed.

---

> 💡 **Pro Tip**: After import, create a "Sprint Planning" view in Linear filtered by Cycle + Team to run daily standups. Use cycle start dates to align with your 2-week sprint cadence.

