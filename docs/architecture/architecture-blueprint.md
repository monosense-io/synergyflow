# Architecture Blueprint — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## C4 — System Context

```mermaid
flowchart LR
  A[Helpdesk Agent]
  D[Developer]
  M[Manager]
  subgraph Internet
    EG[Envoy Gateway]
  end
  subgraph SynergyFlow System
    APP[Backend App - HTTP + SSE]
    WRK[Event Worker]
    TMR[Timer/SLA Service]
    IDX[Search Indexer]
    PG[(PostgreSQL)]
    RS[(Redis Streams)]
    OS[(OpenSearch)]
  end
  KC[(Keycloak)]

  A -->|Use| EG --> APP
  D -->|Use| EG --> APP
  M -->|Use| EG --> APP
  APP <-->|JWT| KC
  APP <-->|CRUD/Queries| PG
  APP -->|SSE Fan-out| A
  WRK <-->|Outbox| PG
  WRK -->|Produce| RS
  APP <-->|Consume| RS
  IDX <-->|Events| RS
  IDX <-->|Index| OS
  TMR <-->|Timers| PG
  TMR -->|Events| RS
```

## C4 — Containers

```mermaid
flowchart TB
  subgraph Backend App [Backend App]
    C1[REST Controllers]
    C2[SSE Controller]
    C3[Module APIs - @NamedInterface]
    C4[Internal Services]
    C5[JPA Repositories]
    C6[Read Models]
  end
  subgraph Companions
    W[Event Worker]
    T[Timer/SLA Service]
    X[Search Indexer]
  end
  DB[(PostgreSQL)]
  RS[(Redis Streams)]
  OS[(OpenSearch)]
  KC[(Keycloak)]

  C1-->C4-->C5-->DB
  C2<-->RS
  W<-->DB
  W-->RS
  T<-->DB
  T-->RS
  X<-->RS
  X-->OS
  C1<-->KC
```

## C4 — Deployment

```mermaid
flowchart LR
  subgraph K8s Cluster
    subgraph Namespace: synergyflow
      DEP1[Deployment: synergyflow-app x5]
      DEP2[Deployment: event-worker x2]
      DEP3[Deployment: timer-sla x2]
      DEP4[Deployment: search-indexer x1]
      SVC[ClusterIP Services]
    end
  end
  EG[Envoy Gateway]
  EG --> SVC --> DEP1
```

---

## Runtime Views

### Queue Load (Read Model → SSE)

```mermaid
sequenceDiagram
  participant UI as AgentConsole
  participant API as Backend App
  participant DB as Postgres (read models)
  participant RS as Redis Streams

  UI->>API: GET /api/itsm/console/queue?cursor=...
  API->>DB: SELECT ticket_card ...
  DB-->>API: rows (≤200ms p95)
  API-->>UI: queue page + nextCursor
  RS-->>API: new TicketUpdated events
  API-->>UI: SSE deltas (id, version, fields)
```

### Side‑Panel Composite

```mermaid
sequenceDiagram
  participant UI as AgentConsole
  participant API as Backend App
  participant DB as Postgres
  participant SPI as SPIs (security/eventing)

  UI->>API: GET /api/itsm/console/sidepanel/{id}
  API->>DB: Read requester history, related incidents
  API->>SPI: Auth batch check, KB/asset stubs
  DB-->>API: data
  SPI-->>API: decisions/context
  API-->>UI: composite bundle (<300ms p95)
```

### Outbox → SSE Fan‑out

```mermaid
sequenceDiagram
  participant APP as Backend App
  participant DB as Postgres (Outbox)
  participant WRK as Event Worker
  participant RS as Redis Streams
  participant UI as Client SSE

  APP->>DB: TX: write domain + outbox {aggregateId,type,version,payload}
  WRK->>DB: poll outbox (at‑least‑once)
  WRK->>RS: append stream event
  APP->>RS: consume event
  APP->>UI: SSE event (id,version,payload)
  UI->>UI: reduce idempotently, drop stale version
```

