# 5. API Specification

## 5.1 REST API Overview

SynergyFlow implements a **RESTful API** following OpenAPI 3.0 specification. All endpoints are versioned (`/api/v1/*`) and protected by JWT authentication via Envoy Gateway.

**Base URL:** `https://synergyflow.example.com/api/v1`

**Authentication:** Bearer token (JWT) in `Authorization` header
```
Authorization: Bearer <jwt-token>
```

**Error Response Format (RFC 7807 Problem Details):**
```json
{
  "type": "https://synergyflow.example.com/errors/validation-error",
  "title": "Validation Error",
  "status": 400,
  "detail": "Priority field is required",
  "instance": "/api/v1/incidents",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-10-18T10:30:00Z"
}
```

## 5.2 REST API Specification (OpenAPI 3.0)

```yaml
openapi: 3.0.0
info:
  title: SynergyFlow API
  version: 1.0.0
  description: Unified ITSM+PM platform with intelligent workflow automation
servers:
  - url: https://synergyflow.example.com/api/v1
    description: Production server
  - url: https://staging.synergyflow.example.com/api/v1
    description: Staging server

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: JWT obtained from OAuth2 provider

  schemas:
    Incident:
      type: object
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
          minLength: 5
          maxLength: 200
        description:
          type: string
          maxLength: 5000
        priority:
          type: string
          enum: [LOW, MEDIUM, HIGH, CRITICAL]
        severity:
          type: string
          enum: [S1, S2, S3, S4]
        status:
          type: string
          enum: [NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED]
        assignedTo:
          type: string
          format: uuid
          nullable: true
        createdBy:
          type: string
          format: uuid
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
        resolvedAt:
          type: string
          format: date-time
          nullable: true
        slaDeadline:
          type: string
          format: date-time
        resolution:
          type: string
          nullable: true
        version:
          type: integer

    Change:
      type: object
      properties:
        id:
          type: string
          format: uuid
        title:
          type: string
        description:
          type: string
        riskLevel:
          type: string
          enum: [LOW, MEDIUM, HIGH, EMERGENCY]
        status:
          type: string
          enum: [REQUESTED, PENDING_APPROVAL, APPROVED, REJECTED, SCHEDULED, IN_PROGRESS, COMPLETED, FAILED, ROLLED_BACK]
        requestedBy:
          type: string
          format: uuid
        approvedBy:
          type: string
          format: uuid
          nullable: true
        scheduledAt:
          type: string
          format: date-time
          nullable: true
        deployedAt:
          type: string
          format: date-time
          nullable: true
        rollbackPlan:
          type: string
        impactedServices:
          type: array
          items:
            type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
        version:
          type: integer

    Task:
      type: object
      properties:
        id:
          type: string
          format: uuid
        projectId:
          type: string
          format: uuid
        parentId:
          type: string
          format: uuid
          nullable: true
        title:
          type: string
        description:
          type: string
        type:
          type: string
          enum: [EPIC, STORY, TASK, SUBTASK]
        status:
          type: string
          enum: [TODO, IN_PROGRESS, DONE]
        priority:
          type: string
          enum: [LOW, MEDIUM, HIGH]
        assignedTo:
          type: string
          format: uuid
          nullable: true
        storyPoints:
          type: integer
          nullable: true
        sprintId:
          type: string
          format: uuid
          nullable: true
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
        completedAt:
          type: string
          format: date-time
          nullable: true
        version:
          type: integer

    Worklog:
      type: object
      properties:
        id:
          type: string
          format: uuid
        userId:
          type: string
          format: uuid
        description:
          type: string
        timeSpent:
          type: integer
          description: Time spent in minutes
        loggedAt:
          type: string
          format: date-time
        incidentId:
          type: string
          format: uuid
          nullable: true
        taskId:
          type: string
          format: uuid
          nullable: true
        createdAt:
          type: string
          format: date-time

    FreshnessBadge:
      type: object
      properties:
        projectionLag:
          type: integer
          description: Projection lag in milliseconds
        thresholdColor:
          type: string
          enum: [GREEN, YELLOW, RED]
          description: "GREEN: <3000ms, YELLOW: 3000-10000ms, RED: >10000ms"
        message:
          type: string
          example: "Data current as of 2.3 seconds ago"

    PaginationMeta:
      type: object
      properties:
        page:
          type: integer
          example: 1
        limit:
          type: integer
          example: 50
        total:
          type: integer
          example: 250
        hasNext:
          type: boolean
          example: true

security:
  - BearerAuth: []

paths:
  # Incident Management Endpoints
  /incidents:
    get:
      summary: List incidents (paginated, filtered)
      tags: [Incidents]
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
        - name: limit
          in: query
          schema:
            type: integer
            default: 50
            maximum: 100
        - name: status
          in: query
          schema:
            type: string
            enum: [NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED]
        - name: priority
          in: query
          schema:
            type: string
            enum: [LOW, MEDIUM, HIGH, CRITICAL]
        - name: assignedTo
          in: query
          schema:
            type: string
            format: uuid
        - name: sort
          in: query
          schema:
            type: string
            default: "createdAt,desc"
            example: "priority,asc"
      responses:
        '200':
          description: List of incidents
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Incident'
                  meta:
                    $ref: '#/components/schemas/PaginationMeta'
                  freshness:
                    $ref: '#/components/schemas/FreshnessBadge'

    post:
      summary: Create new incident
      tags: [Incidents]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [title, description, priority, severity]
              properties:
                title:
                  type: string
                description:
                  type: string
                priority:
                  type: string
                  enum: [LOW, MEDIUM, HIGH, CRITICAL]
                severity:
                  type: string
                  enum: [S1, S2, S3, S4]
      responses:
        '201':
          description: Incident created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Incident'

  /incidents/{id}:
    get:
      summary: Get incident details
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Incident details
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    $ref: '#/components/schemas/Incident'
                  freshness:
                    $ref: '#/components/schemas/FreshnessBadge'

    patch:
      summary: Update incident
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              properties:
                title:
                  type: string
                description:
                  type: string
                priority:
                  type: string
                status:
                  type: string
                resolution:
                  type: string
                version:
                  type: integer
                  description: Optimistic locking version
      responses:
        '200':
          description: Incident updated successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Incident'
        '409':
          description: Optimistic locking conflict

  /incidents/{id}/assign:
    post:
      summary: Assign incident to agent
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [assignedTo]
              properties:
                assignedTo:
                  type: string
                  format: uuid
      responses:
        '200':
          description: Incident assigned successfully

  /incidents/{id}/resolve:
    post:
      summary: Resolve incident
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [resolution]
              properties:
                resolution:
                  type: string
      responses:
        '200':
          description: Incident resolved successfully

  /incidents/{id}/worklogs:
    get:
      summary: Get incident worklogs
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: List of worklogs
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Worklog'

    post:
      summary: Add worklog to incident
      tags: [Incidents]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [description, timeSpent]
              properties:
                description:
                  type: string
                timeSpent:
                  type: integer
                  description: Time spent in minutes
      responses:
        '201':
          description: Worklog added successfully

  /incidents/{id}/related:
    get:
      summary: Get related entities (changes, tasks) for incident
      tags: [Incidents, Cross-Module]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Related entities
          content:
            application/json:
              schema:
                type: object
                properties:
                  changes:
                    type: array
                    items:
                      $ref: '#/components/schemas/Change'
                  tasks:
                    type: array
                    items:
                      $ref: '#/components/schemas/Task'
                  freshness:
                    $ref: '#/components/schemas/FreshnessBadge'

  # Change Management Endpoints
  /changes:
    get:
      summary: List change requests
      tags: [Changes]
      parameters:
        - name: page
          in: query
          schema:
            type: integer
        - name: limit
          in: query
          schema:
            type: integer
        - name: status
          in: query
          schema:
            type: string
        - name: riskLevel
          in: query
          schema:
            type: string
      responses:
        '200':
          description: List of changes
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Change'
                  meta:
                    $ref: '#/components/schemas/PaginationMeta'

    post:
      summary: Create change request
      tags: [Changes]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [title, description, riskLevel, rollbackPlan]
              properties:
                title:
                  type: string
                description:
                  type: string
                riskLevel:
                  type: string
                  enum: [LOW, MEDIUM, HIGH, EMERGENCY]
                rollbackPlan:
                  type: string
                impactedServices:
                  type: array
                  items:
                    type: string
      responses:
        '201':
          description: Change request created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Change'

  /changes/{id}:
    get:
      summary: Get change details
      tags: [Changes]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Change details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Change'

  /changes/{id}/approve:
    post:
      summary: Approve change request
      tags: [Changes]
      description: Approves change request. Auto-approval may occur via OPA policy if low-risk.
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                comments:
                  type: string
      responses:
        '200':
          description: Change approved
        '403':
          description: User not authorized to approve (requires CAB role)

  /changes/{id}/deploy:
    post:
      summary: Mark change as deployed
      tags: [Changes]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [deploymentStatus]
              properties:
                deploymentStatus:
                  type: string
                  enum: [COMPLETED, FAILED]
                deploymentNotes:
                  type: string
      responses:
        '200':
          description: Deployment status updated

  /changes/calendar:
    get:
      summary: Get change calendar view
      tags: [Changes]
      parameters:
        - name: startDate
          in: query
          required: true
          schema:
            type: string
            format: date
        - name: endDate
          in: query
          required: true
          schema:
            type: string
            format: date
      responses:
        '200':
          description: Changes scheduled in date range
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/Change'

  # Task/Project Management Endpoints
  /tasks:
    get:
      summary: List tasks
      tags: [Tasks]
      parameters:
        - name: projectId
          in: query
          schema:
            type: string
            format: uuid
        - name: sprintId
          in: query
          schema:
            type: string
            format: uuid
        - name: status
          in: query
          schema:
            type: string
            enum: [TODO, IN_PROGRESS, DONE]
        - name: assignedTo
          in: query
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: List of tasks
          content:
            application/json:
              schema:
                type: object
                properties:
                  data:
                    type: array
                    items:
                      $ref: '#/components/schemas/Task'
                  meta:
                    $ref: '#/components/schemas/PaginationMeta'

    post:
      summary: Create task
      tags: [Tasks]
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [projectId, title, description, type]
              properties:
                projectId:
                  type: string
                  format: uuid
                parentId:
                  type: string
                  format: uuid
                title:
                  type: string
                description:
                  type: string
                type:
                  type: string
                  enum: [EPIC, STORY, TASK, SUBTASK]
                priority:
                  type: string
                storyPoints:
                  type: integer
      responses:
        '201':
          description: Task created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'

  /tasks/{id}:
    get:
      summary: Get task details
      tags: [Tasks]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Task details
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Task'

    patch:
      summary: Update task
      tags: [Tasks]
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        content:
          application/json:
            schema:
              type: object
              properties:
                title:
                  type: string
                description:
                  type: string
                status:
                  type: string
                assignedTo:
                  type: string
                  format: uuid
                storyPoints:
                  type: integer
                version:
                  type: integer
      responses:
        '200':
          description: Task updated

  # Cross-Module Endpoints (Trust + UX Foundation Pack)
  /worklogs:
    post:
      summary: Single-Entry Time Tray - Log time to incident and/or task
      tags: [Cross-Module, Worklogs]
      description: Logs time that automatically mirrors to both incident and task worklogs if both IDs provided
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [description, timeSpent]
              properties:
                description:
                  type: string
                timeSpent:
                  type: integer
                  description: Time spent in minutes
                incidentId:
                  type: string
                  format: uuid
                  nullable: true
                taskId:
                  type: string
                  format: uuid
                  nullable: true
      responses:
        '201':
          description: Worklog created and mirrored
          content:
            application/json:
              schema:
                type: object
                properties:
                  worklog:
                    $ref: '#/components/schemas/Worklog'
                  mirroredTo:
                    type: array
                    items:
                      type: string
                      enum: [INCIDENT, TASK]
                    example: ["INCIDENT", "TASK"]

  /links:
    post:
      summary: Link-on-Action - Create bidirectional link between entities
      tags: [Cross-Module]
      description: Creates bidirectional link between incident ↔ change, incident ↔ task, etc.
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
              required: [sourceType, sourceId, targetType, targetId]
              properties:
                sourceType:
                  type: string
                  enum: [INCIDENT, CHANGE, TASK]
                sourceId:
                  type: string
                  format: uuid
                targetType:
                  type: string
                  enum: [INCIDENT, CHANGE, TASK]
                targetId:
                  type: string
                  format: uuid
      responses:
        '201':
          description: Link created successfully

  /relationships/{entityType}/{entityId}:
    get:
      summary: Get relationship graph for entity
      tags: [Cross-Module]
      description: Returns all related entities (incidents, changes, tasks) for given entity
      parameters:
        - name: entityType
          in: path
          required: true
          schema:
            type: string
            enum: [incidents, changes, tasks]
        - name: entityId
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Relationship graph
          content:
            application/json:
              schema:
                type: object
                properties:
                  entity:
                    type: object
                    description: Source entity
                  relatedIncidents:
                    type: array
                    items:
                      $ref: '#/components/schemas/Incident'
                  relatedChanges:
                    type: array
                    items:
                      $ref: '#/components/schemas/Change'
                  relatedTasks:
                    type: array
                    items:
                      $ref: '#/components/schemas/Task'
                  freshness:
                    $ref: '#/components/schemas/FreshnessBadge'

  /decision-receipts/{id}:
    get:
      summary: Get decision receipt (explainability)
      tags: [Policy, Audit]
      description: Returns explainable decision receipt for OPA policy evaluation
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: Decision receipt
          content:
            application/json:
              schema:
                type: object
                properties:
                  id:
                    type: string
                    format: uuid
                  policyName:
                    type: string
                    example: "change_approval_policy"
                  policyVersion:
                    type: string
                    example: "v1.2.3"
                  inputData:
                    type: object
                    description: Input data sent to policy
                  decisionOutput:
                    type: object
                    description: Policy decision output
                  explanation:
                    type: string
                    example: "High-risk emergency change impacting critical service → CAB + CTO approval required"
                  correlationId:
                    type: string
                    format: uuid
                  timestamp:
                    type: string
                    format: date-time
```

---
