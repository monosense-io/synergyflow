# ADR-012: Routing Engine and Auto-Assignment Strategy

Date: 2025-10-10
Status: Accepted
Owner: @dev

## Context

Story 2.4 introduces automatic ticket assignment based on configurable routing rules. The system must support category- and priority-based rules, with round-robin assignment within resolver teams. Integration occurs at ticket creation to reduce triage time and improve SLA outcomes.

## Decision

- Implement a stateless `RoutingEngine` domain service responsible for evaluating enabled `RoutingRule` records in ascending `priority` order and assigning tickets on first match.
- Support condition types: `CATEGORY`, `PRIORITY`, `ROUND_ROBIN`. `SUBCATEGORY` is noted for future extension.
- For team rules, select an assignee via a database-side least-loaded heuristic exposed by `TicketCardRepository.findAgentWithFewestTickets(teamId)`; team membership is modeled via `team_members`.
- Integrate the engine into `TicketService.createTicket()` after persistence and SLA creation, and before `TicketCreated` event publication. Failures log a warning without blocking creation.
- Manage team data via a `Team` entity (`teams` table) with UUIDv7 PK and standard audit fields.

## Alternatives Considered

- Application-level load balancing by counting tickets per agent in memory — rejected due to N+1 queries and contention under load.
- Caching routing rules in Redis — deferred as premature optimization (expected rule count <20).
- Making `RoutingEngine` part of `TicketService` — rejected to preserve SRP and testability.

## Consequences

- Clear separation of concerns improves unit testability of rule evaluation logic.
- Database-centric selection ensures correctness under concurrency with a single GROUP BY query.
- Event payloads may include `assigneeId` on creation, allowing downstream consumers to reflect initial assignment.

## Related Artifacts

- Service: `backend/src/main/java/io/monosense/synergyflow/itsm/internal/service/RoutingEngine.java`
- Entity: `backend/src/main/java/io/monosense/synergyflow/itsm/internal/domain/Team.java`
- Repository: `backend/src/main/java/io/monosense/synergyflow/eventing/readmodel/TicketCardRepository.java`
- Migration: `V13__create_teams_table.sql`, `V15__create_team_members_table.sql`
- Tests: `RoutingEngineTest`, `RoutingEngineIntegrationTest`, `TicketCardRepositoryRoundRobinIntegrationTest`

