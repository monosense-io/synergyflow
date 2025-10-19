# 8. Core Workflows

## 8.1 Incident Creation and SLA Tracking Workflow

```mermaid
sequenceDiagram
    actor User as Agent (Web Browser)
    participant FE as Frontend (Next.js)
    participant GW as Envoy Gateway
    participant BE as Backend (Spring Boot)
    participant OPA as OPA Sidecar
    participant DB as PostgreSQL
    participant FW as Flowable Engine
    participant EB as Event Bus (Spring Modulith)
    participant Cache as DragonflyDB

    User->>FE: Fill incident form, submit
    FE->>GW: POST /api/v1/incidents (JWT in header)
    GW->>GW: Validate JWT, check rate limit
    GW->>BE: Forward request
    BE->>BE: Validate request body
    BE->>DB: BEGIN TRANSACTION
    BE->>DB: INSERT INTO incidents
    BE->>DB: INSERT INTO event_publication (IncidentCreatedEvent)
    BE->>DB: COMMIT TRANSACTION
    BE->>EB: Publish IncidentCreatedEvent (in-JVM)
    EB->>BE: Deliver to SLATimerListener
    BE->>FW: Start BPMN process "incident-sla-timer"
    FW->>DB: Persist workflow state (ACT_RU_EXECUTION)
    FW->>FW: Schedule timer (4 hours for P1 incident)
    BE-->>GW: 201 Created (Incident object)
    GW-->>FE: Response
    FE-->>User: Show success toast, redirect to incident detail

    Note over FW: 3h 12m later (80% of SLA)
    FW->>FW: Timer fires (pre-breach notification)
    FW->>BE: Execute service task (sendPreBreachNotification)
    BE->>EB: Publish SLAPreBreachEvent
    EB->>BE: Deliver to NotificationListener
    BE->>User: Send email notification

    Note over FW: 48m later (100% of SLA)
    FW->>FW: Timer fires (SLA breach)
    FW->>BE: Execute service task (escalateIncident)
    BE->>DB: UPDATE incidents SET status='ESCALATED'
    BE->>EB: Publish IncidentEscalatedEvent
```

## 8.2 Change Request with Policy-Driven Approval Workflow

```mermaid
sequenceDiagram
    actor User as DevOps Engineer
    participant FE as Frontend
    participant GW as Envoy Gateway
    participant BE as Backend
    participant OPA as OPA Sidecar
    participant DB as PostgreSQL
    participant EB as Event Bus
    participant FW as Flowable Engine

    User->>FE: Create change request form
    FE->>GW: POST /api/v1/changes
    GW->>BE: Forward request
    BE->>DB: BEGIN TRANSACTION
    BE->>DB: INSERT INTO changes (status=REQUESTED)
    BE->>DB: INSERT INTO event_publication (ChangeRequestedEvent)
    BE->>DB: COMMIT
    BE->>EB: Publish ChangeRequestedEvent
    EB->>BE: Deliver to ChangeApprovalListener

    Note over BE,OPA: Policy Evaluation
    BE->>OPA: POST /v1/data/synergyflow/change_approval_policy
    Note over OPA: Input: {change: {riskLevel: "HIGH", impactedServices: ["API"]}, user: {...}}
    OPA->>OPA: Evaluate Rego policy
    OPA-->>BE: Decision: {allow: false, requiresCAB: true, reason: "High-risk change"}

    BE->>DB: INSERT INTO decision_receipts (policy=change_approval_policy, decision=...)
    BE->>DB: UPDATE changes SET status=PENDING_APPROVAL
    BE->>FW: Start BPMN process "change-approval-workflow"
    FW->>FW: Create user task "CAB Approval" (assigned to CAB members)
    BE-->>User: 201 Created (Change object with status=PENDING_APPROVAL)

    Note over User,FW: 30 minutes later
    actor CAB as CAB Member
    CAB->>FE: Approve change request
    FE->>GW: POST /api/v1/changes/{id}/approve
    GW->>BE: Forward request
    BE->>FW: Complete user task (CAB Approval)
    FW->>BE: Execute next service task (updateChangeStatus)
    BE->>DB: UPDATE changes SET status=APPROVED, approved_by={cabId}
    BE->>EB: Publish ChangeApprovedEvent
    EB->>BE: Deliver to related Incident listeners (if linked)
    BE-->>CAB: 200 OK

    CAB->>User: Notify approval via email/notification
```

## 8.3 Link-on-Action: Create Related Change from Incident

```mermaid
sequenceDiagram
    actor User as Agent
    participant FE as Frontend
    participant GW as Envoy Gateway
    participant BE as Backend
    participant DB as PostgreSQL
    participant EB as Event Bus
    participant Cache as DragonflyDB

    User->>FE: View incident INC-1234, click "Create related change"
    FE->>GW: GET /api/v1/incidents/INC-1234
    GW->>BE: Forward request
    BE->>Cache: GET incident:INC-1234
    Cache-->>BE: Cache miss
    BE->>DB: SELECT * FROM incidents WHERE id=INC-1234
    DB-->>BE: Incident data
    BE->>Cache: SET incident:INC-1234 (TTL 15 min)
    BE-->>FE: Incident object

    FE->>FE: Pre-fill change form with incident context
    Note over FE: Title: "Rollback deployment (incident INC-1234)"<br/>Risk: HIGH (auto-populated from incident severity)<br/>Impacted Services: API-Service (from incident context)

    User->>User: Edit change form, submit
    FE->>GW: POST /api/v1/changes (with incidentId in body)
    GW->>BE: Forward request
    BE->>DB: BEGIN TRANSACTION
    BE->>DB: INSERT INTO changes
    BE->>DB: INSERT INTO incident_changes (incidentId=INC-1234, changeId=CHANGE-890)
    BE->>DB: INSERT INTO event_publication (ChangeRequestedEvent with correlationId)
    BE->>DB: COMMIT
    BE->>EB: Publish ChangeRequestedEvent (contains incidentId canonical ID)
    EB->>BE: Deliver to IncidentChangeListener
    BE->>DB: Update incident related_changes projection
    BE->>Cache: INVALIDATE incident:INC-1234
    BE-->>FE: 201 Created (Change object)

    FE->>User: Show success, display bidirectional link (INC-1234 ↔ CHANGE-890)
```

## 8.4 Single-Entry Time Tray Workflow

```mermaid
sequenceDiagram
    actor User as Agent
    participant FE as Frontend
    participant GW as Envoy Gateway
    participant BE as Backend
    participant DB as PostgreSQL
    participant EB as Event Bus

    User->>FE: Open "Single-Entry Time Tray" (global nav)
    User->>User: Enter time: 30 min, "Analyzed logs, fixed bug"
    User->>User: Select INC-1234 and TASK-890
    FE->>GW: POST /api/v1/worklogs (incidentId=INC-1234, taskId=TASK-890, timeSpent=30)
    GW->>BE: Forward request
    BE->>DB: BEGIN TRANSACTION
    BE->>DB: INSERT INTO worklogs (userId, description, timeSpent, incidentId, taskId)
    BE->>DB: INSERT INTO event_publication (WorklogCreatedEvent)
    BE->>DB: COMMIT
    BE->>EB: Publish WorklogCreatedEvent
    EB->>BE: Deliver to IncidentWorklogListener
    BE->>DB: Update incident aggregate_time_spent
    EB->>BE: Deliver to TaskWorklogListener
    BE->>DB: Update task aggregate_time_spent
    BE-->>FE: 201 Created (Worklog object, mirroredTo: ["INCIDENT", "TASK"])

    FE->>User: Show success toast: "Time logged to INC-1234, TASK-890"
    FE->>FE: Optimistically update incident and task time displays
```

---
