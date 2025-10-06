# Technical Specification: Unified Dashboard & Real-Time Updates

Date: 2025-10-06
Author: monosense
Epic ID: 5
Status: Draft

---

## Overview

Epic 5 delivers a unified cross-module dashboard integrating ITSM tickets and PM issues with real-time Server-Sent Events (SSE) updates. This epic builds on completed ITSM (Epic 2) and PM (Epic 3) modules to provide users with a single pane of glass showing their active work items across both domains. The core technical challenge is implementing a scalable SSE infrastructure that supports 250 concurrent connections with sub-2-second event propagation from database commit to client UI update, while maintaining idempotent client-side reducers to handle out-of-order events and reconnection scenarios.

The dashboard serves as the primary landing page for all user personas (agents, developers, managers, employees), displaying personalized "My Tickets" and "My Issues" views alongside a real-time activity feed announcing assignments, status changes, and mentions. The SSE implementation leverages the existing transactional outbox pattern and Redis Streams established in the architecture, with the Event Worker fanning out events to SSE gateway replicas that maintain persistent connections to browser clients.

## Objectives and Scope

**In Scope:**
- Unified dashboard UI displaying user's active ITSM tickets (from `ticket_card` read model) and PM issues (from `issue_card` read model) in unified card-based layout
- Real-time activity feed showing ticket/issue events (assignments, status transitions, comments, @mentions) filtered by user context
- SSE endpoint (`/stream/events`) with authentication, reconnection support via `Last-Event-ID`, and idempotent event delivery
- Backend SSE gateway module maintaining active connections, subscribing to Redis Streams, and pushing incremental updates to clients
- Client-side SSE consumer with automatic reconnection, cursor-based catch-up (<1s for missed events), and version-based deduplication
- Load testing infrastructure validating 250 concurrent SSE connections sustained for 8 hours without memory leaks
- Responsive dashboard layout: desktop (1440px+) full view with all widgets, tablet (768px+) simplified cards, mobile (<768px) redirect with basic list view
- Empty state handling when user has no active tickets/issues

**Out of Scope (deferred to Phase 2):**
- Push notifications to mobile devices (APNS/FCM)
- Desktop notifications API integration (browser-native)
- Customizable dashboard layouts (drag-drop widgets)
- Advanced filtering/search on dashboard (users navigate to Agent Console/Board for detailed queries)
- Real-time presence indicators ("Alice is viewing Ticket #123") beyond activity feed events
- Offline mode or service worker caching

**Explicitly Not Included:**
- WebSocket bidirectional communication (SSE unidirectional sufficient for event push; commands use REST)
- Polling fallback for browsers without SSE support (evergreen browsers only per NFR15)
- Dashboard widgets for external systems (Slack, email, calendar)

## System Architecture Alignment

This epic extends the existing **Modulith + Companions** architecture with a new **SSE Module** within the Core App and SSE gateway functionality exposed via Envoy Gateway HTTPRoute.

**Module Integration:**
- **SSE Module (new):** Implements SSE endpoint, connection pool management (250+ connections per replica), Redis Stream consumer, and client push logic. Exposes `@NamedInterface` SsePublisher SPI consumed by ITSM and PM modules for event filtering.
- **Eventing Module (existing):** Transactional outbox writes continue as-is. No changes required; SSE consumes events from Redis Streams published by Event Worker.
- **ITSM Module (Epic 2):** Dashboard queries `ticket_card` read model via new composite API `GET /api/itsm/dashboard/my-tickets` returning denormalized cards with SLA, status, priority.
- **PM Module (Epic 3):** Dashboard queries `issue_card` read model via `GET /api/pm/dashboard/my-issues` returning board-ready cards with assignee, sprint, story points.
- **Security Module (existing):** JWT authentication validates SSE connections; RBAC filters dashboard queries to user's authorized tickets/issues.

**Deployment Alignment:**
- **Core App (HTTP + SSE profile):** Existing 5 replicas handle both REST and SSE traffic. Each replica maintains ~50 SSE connections (250 total / 5 replicas). Connection affinity via sticky sessions (Envoy Gateway `cookie` hash) to avoid reconnection storms on pod restart.
- **Event Worker (existing):** No changes. Continues polling outbox, publishing to Redis Stream `events:stream` with consumer group `app-sse` for SSE gateways.
- **Redis Streams:** Single stream `events:stream` with 24-48h retention for reconnect catch-up. Consumer group `app-sse` with 5 consumers (one per Core App replica).

**Infrastructure Constraints:**
- SSE connections are long-lived HTTP connections; Envoy Gateway timeout extended to 10 minutes with keep-alive. Connection drops trigger client exponential backoff reconnect (1s, 2s, 4s, max 30s).
- Each SSE connection consumes ~4KB heap (connection context + buffered events); 250 connections = ~1MB overhead per replica. Well within 2GB request limit.
- Redis Stream lag monitoring added: alert if `app-sse` consumer group lag >5s (target <2s p95 per NFR9).

## Detailed Design

### Services and Modules

| Service/Module | Responsibilities | Inputs | Outputs | Owner |
|---|---|---|---|---|
| **SseController** (new) | Handles SSE endpoint `/stream/events`; authenticates JWT; registers client connection; streams events filtered by user context | HTTP GET with `Authorization: Bearer <jwt>`, optional `Last-Event-ID` header | SSE stream (`text/event-stream`) with JSON event payloads: `{id, type, aggregateId, version, payload}` | Backend (SSE Module) |
| **SseConnectionManager** (new) | Maintains active SSE connection registry; manages connection lifecycle (register, heartbeat, disconnect); routes events to connected clients | `ConnectionId`, `UserId`, `SseEmitter` | Event push to `SseEmitter` instances; connection metrics (count, uptime) | Backend (SSE Module) |
| **SseEventListener** (new) | Subscribes to Redis Stream `events:stream` as consumer group `app-sse`; filters events by user authorization; pushes to `SseConnectionManager` | Redis Stream messages: `{eventId, aggregateId, type, version, occurredAt, payload}` | Filtered events to connection manager | Backend (SSE Module) |
| **DashboardService** (new) | Orchestrates dashboard data aggregation; queries ITSM and PM modules for user's active tickets/issues; returns unified payload | `GET /api/dashboard` with `userId` (from JWT) | Dashboard DTO: `{myTickets: TicketCard[], myIssues: IssueCard[], stats: {openTickets, openIssues}}` | Backend (SSE Module) |
| **ItsmDashboardAdapter** (new) | Provides ITSM-specific dashboard queries; wraps ITSM module's `TicketCardRepository` with user filtering | `userId`, `limit` (default 10) | `TicketCard[]` sorted by SLA urgency then created_at desc | Backend (ITSM Module) |
| **PmDashboardAdapter** (new) | Provides PM-specific dashboard queries; wraps PM module's `IssueCardRepository` with user filtering | `userId`, `limit` (default 10) | `IssueCard[]` sorted by status (In Progress > To Do > Backlog) then priority | Backend (PM Module) |
| **DashboardUI** (new) | React component rendering unified dashboard; fetches initial data from `DashboardService`; establishes SSE connection; applies incremental updates via reducers | Initial load: REST GET; ongoing: SSE events | Rendered dashboard with cards, activity feed, stats | Frontend (Shared UI) |
| **SseClient** (new) | Browser EventSource wrapper; handles connection, reconnection, `Last-Event-ID` cursor tracking; dispatches events to Redux store | SSE stream from `/stream/events` | Redux actions: `{type: 'EVENT_RECEIVED', payload: event}` | Frontend (Shared UI) |
| **DashboardReducer** (new) | Redux reducer applying SSE events to dashboard state; idempotent (drops stale versions); handles ticket/issue updates, new assignments, status changes | Current state + SSE event | Updated state with modified cards, activity feed entries | Frontend (Shared UI) |

**Module Boundaries:**
- SSE Module exposes `@NamedInterface SsePublisher` with method `publishEvent(Event)` - consumed internally by connection manager, never called directly by ITSM/PM (they write to outbox only).
- ITSM/PM modules expose new dashboard query methods via existing `@NamedInterface` APIs (e.g., `ItsmQueryService.getMyActiveTickets(userId)`).
- Security module validates SSE connections via existing `JwtAuthenticationFilter`; no new auth logic required.

### Data Models and Contracts

**Backend Domain Models (existing, no schema changes required):**
- `ticket_card` read model (Epic 2): Already denormalized with `{id, title, status, priority, assignee_id, sla_due_at, created_at}`
- `issue_card` read model (Epic 3): Already denormalized with `{id, title, status, type, assignee_id, sprint_id, story_points, priority, created_at}`

**New Backend DTOs:**

```java
// DashboardResponse DTO
public record DashboardResponse(
    List<TicketCardDto> myTickets,
    List<IssueCardDto> myIssues,
    DashboardStats stats
) {}

public record DashboardStats(
    int openTickets,
    int openIssues,
    int criticalTickets,
    int inProgressIssues
) {}

// SSE Event Envelope (matches outbox schema)
public record SseEvent(
    String id,              // Event ID (UUIDv7, cursor for Last-Event-ID)
    String type,            // Event type: TicketAssigned, IssueStatusChanged, etc.
    String aggregateId,     // TICKET-123, ISSUE-456
    String aggregateType,   // Ticket, Issue
    long version,           // Optimistic lock version
    Instant occurredAt,
    JsonNode payload        // Event-specific data
) {}

// Example Payload for TicketAssigned
{
  "assigneeId": "user-789",
  "assigneeName": "Jane Doe",
  "ticketTitle": "Server Down - DC1",
  "priority": "CRITICAL",
  "reason": "Network expertise"
}
```

**Frontend State Models:**

```typescript
// Dashboard Redux State
interface DashboardState {
  myTickets: TicketCard[];
  myIssues: IssueCard[];
  activityFeed: ActivityEvent[];
  stats: DashboardStats;
  lastEventId: string | null;  // Cursor for SSE reconnect
  connectionStatus: 'connected' | 'disconnected' | 'reconnecting';
}

// Activity Feed Event (derived from SSE)
interface ActivityEvent {
  id: string;
  type: 'ticket_assigned' | 'issue_moved' | 'comment_mentioned';
  timestamp: string;
  message: string;  // "Ticket #1234 assigned to you"
  link: string;     // Deep link to ticket/issue
  read: boolean;
}

// Ticket Card (matches backend DTO)
interface TicketCard {
  id: string;
  title: string;
  status: TicketStatus;
  priority: Priority;
  assigneeId: string | null;
  slaDueAt: string | null;
  slaUrgency: 'safe' | 'warning' | 'critical';  // Client-computed
  createdAt: string;
  version: number;  // For idempotency
}
```

**SSE Protocol:**
- Content-Type: `text/event-stream`
- Event format: `id: <eventId>\ndata: <json>\n\n`
- Heartbeat: Server sends `:\n\n` every 30s to prevent connection timeout
- Reconnection: Client includes `Last-Event-ID: <cursor>` header; server replays missed events from Redis Stream

### APIs and Interfaces

**REST Endpoints:**

**GET /api/dashboard**
- **Description:** Fetch initial dashboard data (user's active tickets + issues + stats)
- **Auth:** JWT required
- **Request:** No body; `userId` extracted from JWT claims
- **Response 200:**
  ```json
  {
    "myTickets": [
      {
        "id": "TICKET-123",
        "title": "Server Down - DC1",
        "status": "IN_PROGRESS",
        "priority": "CRITICAL",
        "assigneeId": "user-789",
        "slaDueAt": "2025-10-06T16:00:00Z",
        "createdAt": "2025-10-06T14:00:00Z",
        "version": 3
      }
    ],
    "myIssues": [
      {
        "id": "ISSUE-456",
        "title": "Implement login API",
        "status": "IN_PROGRESS",
        "type": "STORY",
        "assigneeId": "user-789",
        "sprintId": "sprint-5",
        "storyPoints": 5,
        "priority": "HIGH",
        "createdAt": "2025-10-05T10:00:00Z",
        "version": 2
      }
    ],
    "stats": {
      "openTickets": 3,
      "openIssues": 5,
      "criticalTickets": 1,
      "inProgressIssues": 2
    }
  }
  ```
- **Performance:** p95 <200ms (queries denormalized read models with user filter + limit 10)
- **Errors:** 401 Unauthorized (invalid JWT), 500 (database error)

**SSE Endpoint:**

**GET /stream/events**
- **Description:** Establish SSE connection for real-time event stream
- **Auth:** JWT required in `Authorization: Bearer <token>` header
- **Request Headers:**
  - `Accept: text/event-stream`
  - `Last-Event-ID: <cursor>` (optional, for reconnect catch-up)
- **Response 200:** Stream of events
  ```
  id: 01934f8e-1234-7890-abcd-123456789abc
  data: {"type":"TicketAssigned","aggregateId":"TICKET-123","version":4,"payload":{"assigneeId":"user-789","ticketTitle":"Server Down"}}

  id: 01934f8e-5678-7890-abcd-987654321def
  data: {"type":"IssueStatusChanged","aggregateId":"ISSUE-456","version":3,"payload":{"newStatus":"DONE","actor":"Alice"}}

  :

  ```
- **Heartbeat:** Server sends `:` comment every 30s
- **Reconnect Behavior:** If `Last-Event-ID` provided, server replays events since that cursor from Redis Stream (up to 24h retention)
- **Event Filtering:** Server only sends events relevant to user (assigned tickets/issues, @mentions, team updates based on RBAC)
- **Performance:** Connection setup <500ms; event propagation ≤2s p95 from outbox write to client receive
- **Errors:** 401 Unauthorized, 429 Too Many Connections (>50 per user)

**Internal Module APIs (Spring Modulith `@NamedInterface`):**

```java
// ITSM Module
@NamedInterface("itsm-dashboard-query")
public interface ItsmDashboardQuery {
    List<TicketCardDto> getMyActiveTickets(UserId userId, int limit);
    int countOpenTickets(UserId userId);
    int countCriticalTickets(UserId userId);
}

// PM Module
@NamedInterface("pm-dashboard-query")
public interface PmDashboardQuery {
    List<IssueCardDto> getMyActiveIssues(UserId userId, int limit);
    int countOpenIssues(UserId userId);
    int countInProgressIssues(UserId userId);
}
```

### Workflows and Sequencing

**Sequence 1: Dashboard Initial Load**

```
User → Browser
Browser → DashboardUI.componentDidMount()
DashboardUI → GET /api/dashboard (JWT in header)
API Gateway (Envoy) → DashboardController
DashboardController → SecurityFilter.validateJwt() → extract userId
DashboardController → DashboardService.getDashboard(userId)
DashboardService → ItsmDashboardAdapter.getMyActiveTickets(userId, limit=10)
  ItsmDashboardAdapter → TicketCardRepository.findByAssigneeIdOrderBySlaUrgency(userId)
  TicketCardRepository → PostgreSQL ticket_card table
DashboardService → PmDashboardAdapter.getMyActiveIssues(userId, limit=10)
  PmDashboardAdapter → IssueCardRepository.findByAssigneeIdOrderByStatus(userId)
  IssueCardRepository → PostgreSQL issue_card table
DashboardService → aggregate stats (count queries)
DashboardService ← return DashboardResponse
DashboardController ← return 200 OK with JSON
Browser ← render cards, establish SSE connection
```

**Sequence 2: SSE Connection Establishment**

```
Browser → new EventSource('/stream/events', {headers: {Authorization: 'Bearer <jwt>'}})
Envoy Gateway → SseController.streamEvents(request)
SseController → SecurityFilter.validateJwt() → extract userId
SseController → SseConnectionManager.registerConnection(userId, emitter)
  SseConnectionManager → connectionRegistry.put(connectionId, {userId, emitter, connectedAt})
  SseConnectionManager → emit initial heartbeat ':'
SseController → return SseEmitter (keeps connection open)
Browser ← connection established, readyState = OPEN
Browser → update connectionStatus = 'connected'
```

**Sequence 3: Real-Time Event Propagation**

```
Agent → assigns ticket to user via ITSM UI
ITSM API → TicketService.assignTicket(ticketId, userId)
TicketService → update ticket table + insert outbox event (single transaction)
PostgreSQL ← COMMIT
Event Worker (polling) → detect new outbox row
Event Worker → publish to Redis Stream 'events:stream'
Redis Stream ← message {id, type: TicketAssigned, aggregateId, version, payload}
SseEventListener (consumer group 'app-sse') → receive message
SseEventListener → filter: check if any connected users should receive (assigneeId matches)
SseEventListener → SseConnectionManager.pushToUser(userId, event)
SseConnectionManager → find emitter for userId
SseConnectionManager → emitter.send(SseEmitter.event().id(eventId).data(json))
Browser ← SSE message received
Browser → DashboardReducer.handleEvent(event)
  DashboardReducer → check event.version > currentCard.version (idempotency)
  DashboardReducer → update myTickets array with new card data
  DashboardReducer → prepend activityFeed with "Ticket #123 assigned to you"
DashboardUI ← re-render with yellow highlight animation
```

**Sequence 4: Reconnection with Catch-Up**

```
Browser → connection drops (network hiccup, pod restart)
Browser → EventSource.onerror → close connection
Browser → exponential backoff: wait 1s, then reconnect
Browser → new EventSource('/stream/events', {headers: {Authorization, Last-Event-ID: '<lastCursor>'}})
SseController → extract Last-Event-ID header
SseController → SseEventListener.replayEvents(lastEventId, userId)
  SseEventListener → query Redis Stream from cursor to now (XREAD from lastEventId)
  SseEventListener → filter events for user, emit all missed events
Browser ← receive burst of missed events (could be 0-100 depending on downtime)
DashboardReducer → apply all events, version check prevents duplicates
Browser → update connectionStatus = 'connected'
```

**Error Handling:**
- **401 Unauthorized on SSE:** Close connection, redirect to login
- **429 Too Many Connections:** Show toast "Too many open sessions. Close other tabs."
- **Network error during REST load:** Retry 3x with backoff, then show error banner
- **Redis Stream lag >5s:** Backend alert fires, ops investigates; clients unaware (async tolerated)

## Non-Functional Requirements

### Performance

**API Latency (from PRD NFR1, NFR9):**
- `GET /api/dashboard` p95 <200ms (queries 2x denormalized read models with indexed `assignee_id`, limit 10 rows each)
- `GET /stream/events` connection setup p95 <500ms (JWT validation + connection registration)
- SSE event propagation end-to-end p95 ≤2s from database commit to client UI update (measured as: outbox insert → Event Worker poll → Redis Stream publish → SSE push → client receive)
- SSE reconnection catch-up p95 <1s for up to 100 missed events (Redis Stream XREAD from cursor)

**Concurrency (from PRD NFR2):**
- Support 250 concurrent SSE connections distributed across 5 Core App replicas (~50 connections per replica)
- Each connection consumes ~4KB heap; 250 connections = ~1MB total overhead (negligible vs 2GB request limit per replica)
- SSE connection limit per user: 50 (prevent abuse; typical user has 1-3 tabs open)

**Memory & Resource:**
- Dashboard initial load payload <20KB (10 tickets + 10 issues + stats, gzipped)
- SSE event payload <2KB per event (JSON envelope + small payload)
- Redis Stream retention: 24-48h with max 100k events (estimated ~200MB for 48h at 1 event/s average)
- Connection idle timeout: 10 minutes without heartbeat (Envoy Gateway + client reconnect)

**Load Testing Requirements:**
- Gatling scenario: Ramp 250 concurrent users over 5 minutes, sustain for 8 hours
- Operations mix: 90% SSE idle (receive events), 5% dashboard refresh (F5), 5% tab close/reopen (reconnect)
- Event injection rate: 10 events/sec (simulates 50 agents + 100 developers making state changes)
- Success criteria: p95 latency <2s, zero memory leaks (heap stable after GC), zero dropped events, reconnect <1s

### Security

**Authentication (aligned with PRD NFR11):**
- All endpoints require valid JWT (`Authorization: Bearer <token>`)
- JWT validation uses existing Keycloak OIDC integration (Epic 1)
- SSE connections validate JWT on initial handshake; token refresh requires client reconnect (acceptable given 1h token TTL)

**Authorization (aligned with PRD NFR12, NFR20):**
- Dashboard queries filtered by `userId` extracted from JWT claims; RBAC enforced at SQL level via `WHERE assignee_id = :userId`
- SSE event filtering: Server only pushes events relevant to connected user based on:
  - Assigned tickets/issues (user is assignee)
  - @mentions in comments (user tagged)
  - Team-level updates (user is team member, derived from role)
- Batch authorization overhead <10% of request time (dashboard aggregates 2 queries, no per-row IAM calls)

**Data Protection:**
- SSE connections use HTTPS (TLS 1.2+) enforced by Envoy Gateway
- Event payloads contain minimal PII (assignee name, ticket title); no credit card, SSN, health data
- Internal notes marked as `internal=true` excluded from event payloads for non-agent roles

**Abuse Prevention:**
- Rate limiting: 1000 req/min per user (Envoy Gateway, inherited from Epic 1)
- SSE connection limit: 50 concurrent per user (429 Too Many Connections if exceeded)
- Heartbeat validation: Server closes idle connections (no client activity for 10 minutes)

### Reliability/Availability

**Availability (aligned with PRD NFR3):**
- Target: 99.9% availability (inherits from Core App SLO)
- SSE connections gracefully degrade on backend issues: Connection drops trigger client auto-reconnect with exponential backoff (1s, 2s, 4s, max 30s)
- Dashboard REST fallback: If SSE fails to establish after 3 retries, show banner "Real-time updates unavailable. Refresh page manually."

**Resilience:**
- **Pod Restart:** SSE connections tied to pods drop on restart; clients reconnect to healthy replica via Envoy Gateway load balancing. Sticky sessions minimize thrashing.
- **Redis Stream Unavailable:** SSE listener fails gracefully; existing connections remain open but no new events pushed. Alert fires; ops investigates. Clients unaware (dashboard still functional, just no live updates).
- **Outbox Lag >5s:** Event Worker auto-scales on lag metric; SSE clients tolerate up to 2s p95 propagation delay per NFR9.
- **Duplicate Events:** Client reducers idempotent via version check (`if event.version <= currentCard.version, drop`); Redis Stream at-least-once delivery tolerated.

**Error Handling:**
- 401 Unauthorized (expired JWT): Client closes SSE, redirects to login
- 429 Too Many Connections: Client shows toast "Too many open sessions. Close other tabs.", retries after 30s
- 500 Internal Server Error on dashboard load: Retry 3x with backoff, then show error banner "Unable to load dashboard. Try again later."
- Network partition during SSE: Client detects `onerror` event, reconnects with `Last-Event-ID` cursor

**Disaster Recovery:**
- Redis Stream data loss: Acceptable; clients reconnect and receive events from current timestamp forward (no historical replay needed beyond 24h retention)
- PostgreSQL failure: Dashboard queries fail; existing SSE connections drop (no data to stream); recover via Epic 1 backup/restore strategy (RPO 15min, RTO 4h)

### Observability

**Metrics (aligned with PRD NFR13):**
- **SSE Connection Count:** Gauge per replica and total (target 250 concurrent, alert if >300)
- **SSE Event Push Rate:** Counter tracking events/sec pushed to clients (baseline 1-10 events/sec, spike to 100 during load test)
- **Dashboard API Latency:** Histogram for `GET /api/dashboard` (p50, p95, p99; target p95 <200ms)
- **SSE Propagation Latency:** Histogram measuring outbox write → client receive (p95 ≤2s per NFR9)
- **Reconnection Rate:** Counter tracking reconnections/min (baseline <5/min, spike during pod restarts)
- **Event Drop Rate:** Counter for events not delivered due to connection errors (target 0 under normal conditions)
- **Redis Stream Lag:** Gauge for `app-sse` consumer group lag in seconds (alert if >5s)

**Traces (OpenTelemetry):**
- Dashboard load: `GET /api/dashboard` → ITSM query + PM query spans
- SSE event flow: Outbox insert → Event Worker poll → Redis publish → SSE push (end-to-end trace)
- Reconnection: Client reconnect request → replay query → event burst spans

**Logs (Structured JSON):**
- SSE connection lifecycle: `{event: "sse_connected", userId, connectionId, remoteIp, connectedAt}`
- SSE disconnection: `{event: "sse_disconnected", userId, connectionId, duration, reason}`
- Event push: `{event: "sse_event_pushed", userId, eventType, aggregateId, latency}` (sample 10%)
- Reconnection: `{event: "sse_reconnect", userId, lastEventId, missedEvents, replayDuration}`
- Errors: `{event: "sse_error", userId, error, stackTrace}` (all errors logged)

**Alerts:**
- SSE connection count >300 for 5 minutes → "High SSE connection load"
- SSE propagation latency p95 >3s for 5 minutes → "SSE lag exceeds target"
- Redis Stream lag >5s for 2 minutes → "Event Worker falling behind"
- Dashboard API p95 >400ms for 5 minutes → "Dashboard slow response"
- Reconnection rate >20/min for 5 minutes → "High SSE churn, investigate pod health"

## Dependencies and Integrations

**Epic Dependencies (Blockers):**
- **Epic 1 (Foundation):** MUST be complete for Keycloak authentication, PostgreSQL, Redis infrastructure, Envoy Gateway configuration
- **Epic 2 (ITSM):** MUST be complete for `ticket_card` read model, ITSM module's `@NamedInterface` query APIs
- **Epic 3 (PM):** MUST be complete for `issue_card` read model, PM module's `@NamedInterface` query APIs
- **Eventing Module (implicit in Epic 1):** Transactional outbox and Event Worker MUST be operational for SSE event sourcing

**Backend Technology Stack (from architecture.md):**

| Component | Version | Purpose | Notes |
|---|---|---|---|
| Spring Boot | 3.5.0 | Core framework | Modulith host |
| Spring Modulith | 1.4.2 | Module boundaries | Enforces `@NamedInterface` contracts |
| Spring Data JPA | Boot-managed | Data access | Repository queries for read models |
| Spring MVC | Boot-managed | REST + SSE | `SseEmitter` for streaming |
| PostgreSQL JDBC | 42.7.8 | Database driver | Connection pooling via HikariCP |
| Redisson | 3.52.0 | Redis client | Streams consumer for SSE listener |
| Jackson | Boot-managed | JSON serialization | Event payload serialization |
| Spring Security | Boot-managed | JWT validation | Keycloak OIDC integration |
| OpenTelemetry Java Agent | 2.14.0 | Observability | Tracing SSE event propagation |
| Micrometer | Boot-managed | Metrics | SSE connection count, push rate gauges |

**Frontend Technology Stack (from architecture.md):**

| Component | Version | Purpose | Notes |
|---|---|---|---|
| React | 18.2.0 | UI framework | Functional components + hooks |
| TypeScript | 5.6.x | Type safety | Interface definitions for event DTOs |
| Ant Design | 5.19.x | Component library | Dashboard layout, cards, stats widgets |
| Redux Toolkit | ^2.0.0 | State management | Dashboard reducer, SSE event actions |
| EventSource API | Browser native | SSE client | Polyfill not needed (evergreen browsers only) |
| Axios | ^1.6.0 | HTTP client | Dashboard REST queries |

**Infrastructure Dependencies:**
- **Envoy Gateway:** HTTPRoute for `/stream/events` with extended timeout (10 minutes), sticky sessions (`cookie` hash for connection affinity)
- **Redis Streams:** Consumer group `app-sse` with 5 consumers (one per Core App replica), 24-48h retention
- **PostgreSQL:** No new tables; queries existing `ticket_card`, `issue_card` read models via indexes on `assignee_id`

**External Integrations:**
- **Keycloak:** JWT validation for SSE authentication (existing integration from Epic 1)
- **None:** No third-party push notification services in Phase 1 (deferred to Phase 2)

**Internal Module Integrations:**
- **ITSM Module:** Implements `ItsmDashboardQuery` SPI; exposes `getMyActiveTickets(userId)` for dashboard aggregation
- **PM Module:** Implements `PmDashboardQuery` SPI; exposes `getMyActiveIssues(userId)` for dashboard aggregation
- **Security Module:** Provides `JwtAuthenticationFilter` for SSE authentication; no changes required
- **Eventing Module:** Event Worker publishes to Redis Stream; SSE listener consumes (read-only dependency, no coupling)

## Acceptance Criteria (Authoritative)

Extracted and normalized from PRD Epic 5 acceptance criteria:

**AC-5.1: Unified Dashboard Display**
- **Given** a logged-in user with assigned tickets and issues
- **When** the user navigates to `/dashboard`
- **Then** the page displays two sections: "My Tickets" (up to 10 active ITSM tickets) and "My Issues" (up to 10 active PM issues), each showing card with: ID, title, status, priority, assignee, and for tickets: SLA countdown indicator

**AC-5.2: Dashboard Stats Widget**
- **Given** the dashboard is loaded
- **When** the user views the top stats bar
- **Then** the stats display: total open tickets, total open issues, critical tickets count, in-progress issues count, with color-coded badges (red for critical, amber for medium, green for low)

**AC-5.3: Real-Time Activity Feed**
- **Given** the dashboard is open with active SSE connection
- **When** another user assigns a ticket/issue to the current user, or changes status, or @mentions in comment
- **Then** the activity feed (below stats) displays a new entry within 2 seconds showing: timestamp, event type icon, message ("Ticket #1234 assigned to you by Jane"), and deep link to the ticket/issue

**AC-5.4: SSE Connection Establishment**
- **Given** the dashboard loads successfully
- **When** the UI establishes SSE connection to `/stream/events` with valid JWT
- **Then** the connection opens within 500ms (p95), connection status indicator shows "Connected" (green dot), and initial heartbeat `:` received within 30 seconds

**AC-5.5: Live Card Updates**
- **Given** SSE connection is active
- **When** a ticket/issue in "My Tickets" or "My Issues" changes (status, priority, assignee updated)
- **Then** the affected card updates within 2 seconds with yellow highlight animation (fade over 1 second), and the card's version increments preventing duplicate renders

**AC-5.6: SSE Reconnection with Catch-Up**
- **Given** SSE connection drops (network issue, pod restart)
- **When** the client detects `onerror` event
- **Then** the client reconnects with exponential backoff (1s, 2s, 4s, max 30s), includes `Last-Event-ID` header in reconnect request, receives all missed events (up to 100 events or 24h window), and applies events idempotently without duplicates

**AC-5.7: Responsive Layout**
- **Given** the dashboard is accessed from different devices
- **When** viewport width is ≥1440px (desktop)
- **Then** display full layout with 2-column cards (tickets left, issues right), stats bar with 4 metrics, activity feed on right sidebar
- **When** viewport width is 768-1439px (tablet)
- **Then** display single-column stacked cards (tickets top, issues bottom), stats bar with 2 metrics, activity feed below cards
- **When** viewport width is <768px (mobile)
- **Then** redirect to mobile-optimized list view (out of scope for Epic 5; show "Use desktop for full dashboard" message)

**AC-5.8: Empty State Handling**
- **Given** a user with zero assigned tickets and zero assigned issues
- **When** the dashboard loads
- **Then** display empty state illustrations with messages: "No active tickets" and "No active issues" with CTA buttons "Browse All Tickets" and "Browse All Issues"

**AC-5.9: Load Test (8-Hour Soak)**
- **Given** Gatling load test scenario with 250 concurrent SSE connections
- **When** the test runs for 8 hours with 10 events/sec injection rate
- **Then** all connections remain stable (no disconnects except simulated network issues), p95 event propagation <2s, heap usage stable after GC (no memory leaks), zero events dropped, reconnection p95 <1s

**AC-5.10: Error Handling**
- **Given** various error scenarios
- **When** JWT expires during SSE connection
- **Then** server sends 401, client closes connection and redirects to login
- **When** user exceeds 50 concurrent SSE connections
- **Then** server returns 429 with Retry-After header, client shows toast "Too many open sessions"
- **When** dashboard REST API fails after 3 retries
- **Then** show error banner "Unable to load dashboard. Refresh to try again." with retry button

## Traceability Mapping

| AC ID | Spec Section(s) | Component(s)/API(s) | Test Idea |
|---|---|---|---|
| AC-5.1 | Services (DashboardService), APIs (GET /api/dashboard), Data Models (DashboardResponse) | `DashboardController`, `ItsmDashboardAdapter`, `PmDashboardAdapter`, `DashboardUI` component | Integration test: Mock user with 5 tickets + 3 issues, assert REST response contains correct cards with all fields |
| AC-5.2 | Services (DashboardService), Data Models (DashboardStats) | `DashboardService.getDashboard()`, stats aggregation logic | Unit test: Given tickets/issues in various statuses, assert stats counts match |
| AC-5.3 | Workflows (Sequence 3: Real-Time Event Propagation), APIs (GET /stream/events) | `SseEventListener`, `SseConnectionManager`, `DashboardReducer` | E2E test: User A assigns ticket to User B, verify activity feed entry appears in User B's dashboard within 2s |
| AC-5.4 | APIs (GET /stream/events), Services (SseController, SseConnectionManager) | `SseController.streamEvents()`, `SseConnectionManager.registerConnection()` | Integration test: Call SSE endpoint with valid JWT, assert connection opens <500ms, heartbeat received |
| AC-5.5 | Workflows (Sequence 3), Data Models (version field) | `DashboardReducer.handleEvent()`, idempotency logic | Unit test: Dispatch 2 events with same version, assert reducer only updates once |
| AC-5.6 | Workflows (Sequence 4: Reconnection with Catch-Up), APIs (Last-Event-ID header) | `SseEventListener.replayEvents()`, Redis Stream XREAD | Integration test: Disconnect client, inject 10 events, reconnect with cursor, assert 10 events replayed |
| AC-5.7 | Frontend (DashboardUI component), CSS media queries | Ant Design Grid layout, responsive breakpoints | Visual regression test: Percy snapshots at 1440px, 768px, 375px viewports |
| AC-5.8 | Frontend (DashboardUI empty state) | `DashboardUI` conditional rendering | Component test: Mount DashboardUI with empty arrays, assert empty state message + CTA buttons render |
| AC-5.9 | NFR Performance (Load Testing Requirements) | End-to-end system | Gatling script: `DashboardLoadTest.scala` with 250 users, 8h duration, assert p95 <2s, heap stable |
| AC-5.10 | APIs (error codes 401, 429, 500), Workflows (error handling) | `SseController` error responses, client error handlers | Integration test: Mock JWT expiry, assert 401 response + client redirect; mock connection limit, assert 429 + toast |

## Risks, Assumptions, Open Questions

**Risks:**

1. **Risk: SSE Connection Instability on Mobile Networks**
   - **Impact:** Mobile users (agents in field) experience frequent reconnections on cellular networks with spotty coverage
   - **Likelihood:** Medium (mobile browsers support SSE, but network quality varies)
   - **Mitigation:** Implement aggressive exponential backoff (max 30s) to avoid battery drain; consider Phase 2 mobile app with native push notifications as alternative
   - **Owner:** Frontend Lead

2. **Risk: Redis Stream Consumer Group Lag Under High Load**
   - **Impact:** SSE event propagation exceeds 2s p95 target during peak usage (e.g., Monday morning rush with 250 concurrent agents)
   - **Likelihood:** Low (Gatling tests validate 250 users, but real-world spikes unpredictable)
   - **Mitigation:** Implement HPA (Horizontal Pod Autoscaler) for Event Worker scaling on `outbox_rows_behind` metric; add Redis Stream lag alerting (<5s threshold)
   - **Owner:** DevOps + Backend Architect

3. **Risk: Browser EventSource API Limitations**
   - **Impact:** EventSource does not support custom headers in all browsers (authorization header workaround needed), no built-in reconnect backoff control
   - **Likelihood:** Medium (EventSource spec limitation)
   - **Mitigation:** Use query param `?token=<jwt>` as fallback if header fails (security risk mitigated by short-lived tokens); implement custom reconnect logic with exponential backoff in wrapper
   - **Owner:** Frontend Lead

4. **Risk: Sticky Session Failures Cause Reconnection Storms**
   - **Impact:** Envoy Gateway sticky session cookie fails (user clears cookies, incognito mode), causing client to round-robin across replicas and lose connection context
   - **Likelihood:** Low (sticky sessions work reliably in most cases)
   - **Mitigation:** Stateless SSE design: Connection manager stores minimal state (userId → emitter mapping); reconnect logic tolerates switching replicas via `Last-Event-ID` cursor
   - **Owner:** Backend Architect

**Assumptions:**

1. **Assumption:** Users tolerate 2s event propagation delay (not instantaneous real-time like collaborative editing)
   - **Validation:** Confirmed with stakeholders; ITSM/PM workflows not latency-critical like chat or gaming
   - **Impact if False:** Would require WebSocket upgrade or Redis pub/sub for <500ms latency

2. **Assumption:** 250 concurrent users is peak load; no need for horizontal scaling beyond 5 Core App replicas
   - **Validation:** Based on user segmentation (50 agents + 100 developers peak concurrent)
   - **Impact if False:** Add HPA for Core App scaling on SSE connection count metric

3. **Assumption:** 24-48h Redis Stream retention sufficient for reconnection catch-up (users don't leave tabs open for days offline)
   - **Validation:** Typical user session <8 hours; overnight disconnects replay from morning's events
   - **Impact if False:** Increase retention to 7 days (storage cost +200MB)

4. **Assumption:** Browser EventSource API available in all target browsers (Chrome, Firefox, Safari, Edge - last 2 versions)
   - **Validation:** MDN confirms EventSource supported in all evergreen browsers since 2015
   - **Impact if False:** N/A (NFR15 excludes IE11, which lacks EventSource)

**Open Questions:**

1. **Q: Should activity feed persist across sessions (store in database) or in-memory only?**
   - **Context:** Current design: Activity feed derived from SSE events in-memory; cleared on page refresh
   - **Options:** (A) In-memory (simpler, no DB writes), (B) Persist last 50 events per user in `activity_log` table
   - **Decision Needed By:** Week 14 (Sprint 7) before frontend implementation
   - **Owner:** Product Manager (John) to decide based on user feedback

2. **Q: How to handle SSE connection during JWT token refresh (1h TTL)?**
   - **Context:** JWT expires after 1 hour; SSE connection validated at handshake only
   - **Options:** (A) Force reconnect every 55 minutes (proactive), (B) Wait for 401 on next event push (reactive), (C) Implement token refresh endpoint and inject refresh event via SSE
   - **Decision Needed By:** Week 14 (Sprint 7) during SSE implementation
   - **Owner:** Backend Architect (security team to consult)

3. **Q: Should dashboard display all assigned tickets/issues or only "active" ones (exclude Closed/Done)?**
   - **Context:** PRD says "active" but definition unclear
   - **Options:** (A) Active = not Closed/Done, (B) Active = In Progress + To Do only, (C) User-configurable filter
   - **Decision Needed By:** Week 13 (Sprint 7) before dashboard UI design finalized
   - **Owner:** UX Expert (Sally) + Product Manager (John)

4. **Q: What is max event burst size during reconnection (limit to prevent UI freeze)?**
   - **Context:** User disconnects for 1 hour, 600 events queued (10/sec * 60min * 60sec); replaying all at once may freeze browser
   - **Options:** (A) Replay all (trust reducer performance), (B) Limit to last 100 events + full dashboard refresh, (C) Paginated catch-up with progress indicator
   - **Decision Needed By:** Week 15 (Sprint 8) before load testing
   - **Owner:** Frontend Lead (performance testing to inform decision)

## Test Strategy Summary

**Unit Tests (JUnit 5 + Mockito for backend, Vitest for frontend):**
- **Backend:** Test `SseConnectionManager` connection lifecycle (register, heartbeat, disconnect), `SseEventListener` event filtering logic (user authorization), `DashboardService` aggregation (ITSM + PM queries)
- **Frontend:** Test `DashboardReducer` idempotency (version checks), SSE client reconnect logic (exponential backoff), activity feed message formatting
- **Coverage Target:** >80% for SSE module, dashboard service, reducers

**Integration Tests (Spring Boot Test + Testcontainers):**
- **SSE Endpoint:** Test `GET /stream/events` with valid JWT → assert connection opens, heartbeat sent, events pushed on Redis Stream publish
- **Dashboard API:** Test `GET /api/dashboard` → mock ITSM/PM read models, assert aggregated response with correct tickets/issues
- **Reconnection:** Test client disconnect → inject events → reconnect with `Last-Event-ID` → assert missed events replayed from Redis Stream
- **Error Cases:** Test 401 (expired JWT), 429 (too many connections), 500 (database down) → assert correct error codes and client handling

**E2E Tests (Playwright):**
- **Happy Path:** User logs in → dashboard loads with tickets/issues → colleague assigns new ticket → activity feed updates within 2s with yellow highlight
- **Reconnection:** Open dashboard → kill backend pod → verify client reconnects → assign ticket → verify event received after reconnect
- **Responsive:** Load dashboard at 1440px → verify 2-column layout → resize to 768px → verify single-column layout
- **Empty State:** Login as user with no assignments → verify empty state messages + CTA buttons

**Performance Tests (Gatling):**
- **Baseline:** 50 users, 10 minutes, measure p95 dashboard load <200ms, SSE connection <500ms, event propagation <2s
- **Load Test:** 250 concurrent users, 8 hours, operations mix (90% SSE idle, 5% refresh, 5% reconnect), 10 events/sec injection
- **Soak Test:** 250 users, 8 hours continuous, monitor heap usage (no memory leaks), connection count stable, zero dropped events
- **Stress Test:** Ramp to 500 users (2x capacity) to identify breaking point; expect degradation but graceful (no cascading failures)

**Security Tests:**
- **AuthN:** Test SSE endpoint without JWT → assert 401 Unauthorized
- **AuthZ:** Test user A receives only events for their assigned tickets/issues (not user B's)
- **Rate Limiting:** Test 1001 requests/min → assert 429 from Envoy Gateway
- **Connection Limit:** Test user opens 51 SSE connections → assert 51st returns 429

**Accessibility Tests (axe-core):**
- Dashboard cards have ARIA labels for status/priority badges
- Activity feed has `aria-live="polite"` for screen reader announcements
- Keyboard navigation: Tab through cards, Enter to open deep link

**Load Soak Validation (8-Hour Test - AC-5.9):**
- **Setup:** Deploy to staging with 5 Core App replicas, Redis Stream, PostgreSQL
- **Execution:** Run Gatling script `DashboardLoadTest.scala` with 250 users, 10 events/sec for 8 hours
- **Success Criteria:**
  - p95 event propagation ≤2s (measure via OpenTelemetry trace: outbox insert → client receive)
  - Zero memory leaks (heap stable after GC, no OOMEs)
  - Zero dropped events (compare events published to Redis vs events received by clients)
  - Reconnection p95 <1s (measure reconnect request → first event received)
  - SSE connection count stable at 250±5 (account for normal churn)
- **Monitoring:** Prometheus dashboards for SSE metrics, Grafana heap usage graphs, Redis Stream lag alerts
- **Pass/Fail Gate:** All criteria met → Epic 5 approved for production; any failure → investigate root cause, optimize, re-run
