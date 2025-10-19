# 4. Data Models

## 4.1 Incident

**Purpose:** Represents an unplanned interruption or reduction in quality of an IT service, tracked from creation through resolution and closure.

**Key Attributes:**
- `id`: UUID - Unique identifier for the incident (canonical ID propagated across modules)
- `title`: String - Brief description of the incident
- `description`: String - Detailed description of the issue
- `priority`: Enum (LOW, MEDIUM, HIGH, CRITICAL) - Business urgency
- `severity`: Enum (S1, S2, S3, S4) - Technical impact (S1 = service down, S4 = minor)
- `status`: Enum (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED) - Lifecycle state
- `assignedTo`: UUID (User ID) - Currently assigned agent
- `createdBy`: UUID (User ID) - User who created the incident
- `createdAt`: Timestamp - Creation timestamp
- `updatedAt`: Timestamp - Last update timestamp
- `resolvedAt`: Timestamp (nullable) - Resolution timestamp
- `slaDeadline`: Timestamp - SLA breach deadline (calculated based on priority)
- `resolution`: String (nullable) - Resolution notes
- `version`: Integer - Optimistic locking version field

### TypeScript Interface

```typescript
export interface Incident {
  id: string;
  title: string;
  description: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  severity: 'S1' | 'S2' | 'S3' | 'S4';
  status: 'NEW' | 'ASSIGNED' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  assignedTo: string | null;
  createdBy: string;
  createdAt: string; // ISO-8601 timestamp
  updatedAt: string;
  resolvedAt: string | null;
  slaDeadline: string;
  resolution: string | null;
  version: number;
}
```

### Relationships
- `1-to-Many` with **Worklog** (incident worklogs)
- `1-to-Many` with **Comment** (incident comments)
- `Many-to-Many` with **Change** (related changes via `incident_changes` join table)
- `Many-to-Many` with **Task** (related tasks via `incident_tasks` join table)
- `Many-to-1` with **Problem** (incident linked to problem record)

## 4.2 Change

**Purpose:** Represents a change request for modifying IT infrastructure, applications, or services, with approval workflow and deployment tracking.

**Key Attributes:**
- `id`: UUID - Unique identifier for the change request
- `title`: String - Change summary
- `description`: String - Detailed change plan
- `riskLevel`: Enum (LOW, MEDIUM, HIGH, EMERGENCY) - Risk assessment
- `status`: Enum (REQUESTED, PENDING_APPROVAL, APPROVED, REJECTED, SCHEDULED, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK) - Lifecycle state
- `requestedBy`: UUID (User ID) - Change requester
- `approvedBy`: UUID (User ID, nullable) - Approver (CAB member or auto-approved policy)
- `scheduledAt`: Timestamp (nullable) - Scheduled deployment time
- `deployedAt`: Timestamp (nullable) - Actual deployment time
- `rollbackPlan`: String - Rollback procedure
- `impactedServices`: Array<String> - List of impacted service names
- `createdAt`: Timestamp - Creation timestamp
- `updatedAt`: Timestamp - Last update timestamp
- `version`: Integer - Optimistic locking version field

### TypeScript Interface

```typescript
export interface Change {
  id: string;
  title: string;
  description: string;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'EMERGENCY';
  status: 'REQUESTED' | 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'ROLLED_BACK';
  requestedBy: string;
  approvedBy: string | null;
  scheduledAt: string | null;
  deployedAt: string | null;
  rollbackPlan: string;
  impactedServices: string[];
  createdAt: string;
  updatedAt: string;
  version: number;
}
```

### Relationships
- `Many-to-Many` with **Incident** (related incidents via `incident_changes` join table)
- `Many-to-1` with **Release** (change part of release)
- `1-to-Many` with **DecisionReceipt** (approval policy decisions)

## 4.3 Task

**Purpose:** Represents a unit of work in a project management context (Story, Task, Subtask), tracked through sprint planning and completion.

**Key Attributes:**
- `id`: UUID - Unique identifier for the task
- `projectId`: UUID - Parent project ID
- `parentId`: UUID (nullable) - Parent task ID (for subtasks, story → task hierarchy)
- `title`: String - Task summary
- `description`: String - Detailed task description
- `type`: Enum (EPIC, STORY, TASK, SUBTASK) - Task hierarchy level
- `status`: Enum (TODO, IN_PROGRESS, DONE) - Kanban board status
- `priority`: Enum (LOW, MEDIUM, HIGH) - Priority
- `assignedTo`: UUID (User ID, nullable) - Assigned developer
- `storyPoints`: Integer (nullable) - Estimation (Fibonacci: 1, 2, 3, 5, 8, 13)
- `sprintId`: UUID (nullable) - Assigned sprint
- `createdAt`: Timestamp - Creation timestamp
- `updatedAt`: Timestamp - Last update timestamp
- `completedAt`: Timestamp (nullable) - Completion timestamp
- `version`: Integer - Optimistic locking version field

### TypeScript Interface

```typescript
export interface Task {
  id: string;
  projectId: string;
  parentId: string | null;
  title: string;
  description: string;
  type: 'EPIC' | 'STORY' | 'TASK' | 'SUBTASK';
  status: 'TODO' | 'IN_PROGRESS' | 'DONE';
  priority: 'LOW' | 'MEDIUM' | 'HIGH';
  assignedTo: string | null;
  storyPoints: number | null;
  sprintId: string | null;
  createdAt: string;
  updatedAt: string;
  completedAt: string | null;
  version: number;
}
```

### Relationships
- `Many-to-1` with **Project** (task belongs to project)
- `Self-referential 1-to-Many` (parent task → subtasks)
- `Many-to-1` with **Sprint** (task assigned to sprint)
- `Many-to-Many` with **Incident** (related incidents via `incident_tasks` join table)
- `1-to-Many` with **Worklog** (task worklogs)

## 4.4 Worklog

**Purpose:** Represents time logged against an incident or task, supporting Single-Entry Time Tray feature (log once, mirrors to both incidents and tasks).

**Key Attributes:**
- `id`: UUID - Unique identifier for the worklog entry
- `userId`: UUID - User who logged the work
- `description`: String - Work performed description
- `timeSpent`: Integer - Time spent in minutes
- `loggedAt`: Timestamp - When work was logged
- `incidentId`: UUID (nullable) - Related incident ID
- `taskId`: UUID (nullable) - Related task ID
- `createdAt`: Timestamp - Creation timestamp

**Note:** Single worklog entry can be associated with BOTH incidentId and taskId (Single-Entry Time Tray feature).

### TypeScript Interface

```typescript
export interface Worklog {
  id: string;
  userId: string;
  description: string;
  timeSpent: number; // minutes
  loggedAt: string;
  incidentId: string | null;
  taskId: string | null;
  createdAt: string;
}
```

### Relationships
- `Many-to-1` with **User** (user who logged work)
- `Many-to-1` with **Incident** (related incident, nullable)
- `Many-to-1` with **Task** (related task, nullable)

## 4.5 User

**Purpose:** Represents a platform user (agent, developer, CAB member, end user) with authentication, authorization, and team assignment.

**Key Attributes:**
- `id`: UUID - Unique identifier for the user
- `email`: String - Email address (unique)
- `name`: String - Full name
- `role`: Enum (ADMIN, AGENT, DEVELOPER, CAB_MEMBER, END_USER) - Primary role
- `teamId`: UUID (nullable) - Assigned team
- `skills`: Array<String> - User skills (e.g., ["Java", "Networking", "Database"])
- `capacity`: Integer - Weekly capacity in hours (default 40)
- `status`: Enum (ACTIVE, INACTIVE) - Account status
- `createdAt`: Timestamp - Creation timestamp
- `updatedAt`: Timestamp - Last update timestamp

### TypeScript Interface

```typescript
export interface User {
  id: string;
  email: string;
  name: string;
  role: 'ADMIN' | 'AGENT' | 'DEVELOPER' | 'CAB_MEMBER' | 'END_USER';
  teamId: string | null;
  skills: string[];
  capacity: number; // hours/week
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
  updatedAt: string;
}
```

### Relationships
- `Many-to-1` with **Team** (user assigned to team)
- `1-to-Many` with **Incident** (incidents assigned to user)
- `1-to-Many` with **Task** (tasks assigned to user)
- `1-to-Many` with **Worklog** (worklogs created by user)

## 4.6 DecisionReceipt

**Purpose:** Stores explainable decision receipts for all OPA policy evaluations, supporting "audits like a bank" philosophy with 100% coverage.

**Key Attributes:**
- `id`: UUID - Unique identifier for the decision receipt
- `policyName`: String - OPA policy name (e.g., "change_approval_policy")
- `policyVersion`: String - Policy version (e.g., "v1.2.3")
- `inputData`: JSONB - Input data sent to policy (incident, user, change, etc.)
- `decisionOutput`: JSONB - Policy decision output (allow, deny, reasons, etc.)
- `explanation`: String - Human-readable explanation (e.g., "High-risk emergency change → CAB approval required")
- `correlationId`: UUID - Correlation ID for request tracing
- `timestamp`: Timestamp - Decision timestamp

### TypeScript Interface

```typescript
export interface DecisionReceipt {
  id: string;
  policyName: string;
  policyVersion: string;
  inputData: Record<string, any>; // JSONB
  decisionOutput: Record<string, any>; // JSONB
  explanation: string;
  correlationId: string;
  timestamp: string;
}
```

### Relationships
- `Many-to-1` with **Change** (change approval decisions)
- `Many-to-1` with **Incident** (incident assignment/routing decisions)

## 4.7 EventPublication (Spring Modulith)

**Purpose:** Transactional outbox table storing events atomically with aggregate writes, enabling durable at-least-once event delivery.

**Key Attributes:**
- `id`: UUID - Unique identifier for the event publication
- `eventType`: String - Event type (e.g., "IncidentCreatedEvent", "ChangeApprovedEvent")
- `aggregateId`: UUID - Canonical ID of the aggregate (incidentId, changeId, taskId)
- `eventData`: JSONB - Serialized event payload (Java record serialized to JSON)
- `publishedAt`: Timestamp - When event was published (same transaction as aggregate write)
- `completedAt`: Timestamp (nullable) - When all listeners completed processing
- `listenerId`: String (nullable) - Listener ID for completion tracking
- `retryCount`: Integer - Number of retry attempts
- `correlationId`: UUID - Correlation ID for request tracing
- `causationId`: UUID (nullable) - Previous event that caused this event

**Note:** This table is managed by Spring Modulith, not manually created.

### TypeScript Interface

```typescript
export interface EventPublication {
  id: string;
  eventType: string;
  aggregateId: string;
  eventData: Record<string, any>; // JSONB
  publishedAt: string;
  completedAt: string | null;
  listenerId: string | null;
  retryCount: number;
  correlationId: string;
  causationId: string | null;
}
```

---
