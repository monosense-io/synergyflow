# Data Model v1 — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## ERD (High Level)

```mermaid
erDiagram
  USERS ||--o{ TICKETS : requests
  USERS ||--o{ ISSUES : reports
  TICKETS ||--o{ TICKET_COMMENTS : has
  ISSUES ||--o{ ISSUE_COMMENTS : has
  ISSUES ||--o{ ISSUE_LINKS : linked
  BOARDS ||--o{ BOARD_COLUMNS : contains
  SPRINTS ||--o{ ISSUES : includes

  TICKETS {
    uuid id PK
    string ticket_type
    string title
    string status
    string priority
    uuid requester_id FK
    uuid assignee_id
    bigint version
    timestamptz created_at
    timestamptz updated_at
  }
  ISSUES {
    uuid id PK
    string type
    string title
    string status
    string priority
    uuid reporter_id FK
    uuid assignee_id
    int story_points
    uuid sprint_id FK
    uuid epic_id
    bigint version
    timestamptz created_at
    timestamptz updated_at
  }
```

## Read Models (CQRS‑lite)

- `ticket_card`: queue rows for ITSM (denormalized, ≤6KB/row typical)
- `queue_row`: list view backing store
- `issue_card`: board card projection
- `board_view`: board with columns and ordered card IDs
- `sprint_summary`: burndown data points

## UUID Policy

- All primary keys are UUIDv7 (see `docs/uuidv7-implementation-guide.md`).

## Migrations

- Initial Flyway DDL at `docs/architecture/db/migrations/V1__core_schema.sql`.
