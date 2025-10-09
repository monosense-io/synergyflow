# Module Boundaries — SynergyFlow

Date: 2025-10-06
Owner: Architect

---

## Allowed Dependencies Matrix

| Module | May Depend On |
|---|---|
| itsm | security, eventing, sla, sse, audit |
| pm | security, eventing, sla, sse, audit |
| workflow | security, eventing, sla, sse, audit, itsm?, pm? (via SPI only) |
| security | audit |
| eventing | audit |
| sla | eventing, audit |
| sse | eventing, security |
| audit | (none) |

Notes:
- Cross‑module calls must go via `spi/` packages annotated with `@NamedInterface`.
- `internal/` packages are not referenced from other modules; access enforced by tests.

## @NamedInterface Catalog

- `io.monosense.synergyflow.itsm.spi.TicketQueryService`
- `io.monosense.synergyflow.pm.spi.IssueQueryService`
- `io.monosense.synergyflow.workflow.spi.ApprovalQueryService`
- `io.monosense.synergyflow.security.api.BatchAuthService`
- `io.monosense.synergyflow.eventing.api.EventPublisher`
- `io.monosense.synergyflow.sla.api.TimerScheduler`
- `io.monosense.synergyflow.sse.api.SseBroadcaster`
- `io.monosense.synergyflow.audit.api.AuditLogWriter`

## Enforcement Approach

Spring Modulith module declarations:

```java
// io/monosense/synergyflow/itsm/package-info.java
@org.springframework.modulith.ApplicationModule(displayName = "ITSM Module")
package io.monosense.synergyflow.itsm;
```

```java
// io/monosense/synergyflow/itsm/spi/package-info.java
@org.springframework.modulith.NamedInterface
package io.monosense.synergyflow.itsm.spi;
```

Modularity test:

```java
// backend/src/test/java/io/monosense/synergyflow/ModularityTests.java
@Test
void verifiesModularity() {
  ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);
  modules.verify();
}
```

ArchUnit rule (internal access):

```java
// Enforce no access to internal/
noClasses().that().resideOutsideOfPackage("..internal..")
  .should().accessClassesThat().resideInAnyPackage("..internal..")
  .because("Internal packages are implementation details");
```

## SPI Usage Examples

### TicketQueryService SPI (ITSM Module)

The `TicketQueryService` SPI allows cross-module ticket queries without violating module boundaries. Other modules (PM, Workflow, Dashboard) can inject this service to query ticket data using DTOs, never accessing internal ITSM entities.

**SPI Contract Stability**: This interface is a stable API boundary. Breaking changes require deprecation cycle (min 2 releases). Clients depend on this contract for integration.

#### Example 1: PM Module - Link Tasks to Tickets

```java
package io.monosense.synergyflow.pm.internal.service;

import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import io.monosense.synergyflow.itsm.spi.TicketStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PM module service linking project tasks to ITSM tickets.
 * Uses TicketQueryService SPI to query ticket status without coupling to ITSM internals.
 */
@Service
class TaskTicketLinkService {

    private final TicketQueryService ticketQueryService;

    // Constructor injection of cross-module SPI
    public TaskTicketLinkService(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    /**
     * Checks if a task can be closed based on linked ticket status.
     * Task closure blocked if any linked tickets are not RESOLVED or CLOSED.
     */
    public boolean canCloseTask(UUID taskId, List<UUID> linkedTicketIds) {
        for (UUID ticketId : linkedTicketIds) {
            Optional<TicketSummaryDTO> ticket = ticketQueryService.findById(ticketId);

            if (ticket.isEmpty()) {
                // Linked ticket not found - block closure
                return false;
            }

            TicketStatus status = ticket.get().status();
            if (status != TicketStatus.RESOLVED && status != TicketStatus.CLOSED) {
                // Ticket still open - block task closure
                return false;
            }
        }

        return true; // All linked tickets resolved or closed
    }

    /**
     * Retrieves summary of all tickets linked to a project for dashboard display.
     */
    public List<TicketSummaryDTO> getProjectTickets(UUID projectId, List<UUID> ticketIds) {
        return ticketIds.stream()
                .map(ticketQueryService::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }
}
```

#### Example 2: Workflow Module - Query Ticket Status for Approval Routing

```java
package io.monosense.synergyflow.workflow.internal.service;

import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import io.monosense.synergyflow.itsm.spi.Priority;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Workflow module service determining approval routing based on ticket priority.
 * Uses TicketQueryService SPI to query ticket data for workflow decisions.
 */
@Service
class ApprovalRoutingService {

    private final TicketQueryService ticketQueryService;

    public ApprovalRoutingService(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    /**
     * Determines if manager approval is required based on ticket priority.
     * CRITICAL and HIGH priority tickets require manager approval.
     */
    public boolean requiresManagerApproval(UUID ticketId) {
        Optional<TicketSummaryDTO> ticket = ticketQueryService.findById(ticketId);

        if (ticket.isEmpty()) {
            return false; // Ticket not found, no approval required
        }

        Priority priority = ticket.get().priority();
        return priority == Priority.CRITICAL || priority == Priority.HIGH;
    }
}
```

#### Example 3: Dashboard Module - Aggregate Ticket Metrics with Search Criteria

```java
package io.monosense.synergyflow.dashboard.internal.service;

import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSearchCriteria;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import io.monosense.synergyflow.itsm.spi.TicketStatus;
import io.monosense.synergyflow.itsm.spi.Priority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dashboard module service aggregating ticket metrics for reporting.
 * Uses TicketQueryService SPI with TicketSearchCriteria for complex queries.
 */
@Service
class TicketMetricsAggregator {

    private final TicketQueryService ticketQueryService;

    public TicketMetricsAggregator(TicketQueryService ticketQueryService) {
        this.ticketQueryService = ticketQueryService;
    }

    /**
     * Calculates agent workload (count of tickets by status).
     */
    public Map<TicketStatus, Long> getAgentWorkload(UUID agentId) {
        List<TicketSummaryDTO> tickets = ticketQueryService.findByAssignee(agentId);

        return tickets.stream()
                .collect(Collectors.groupingBy(
                        TicketSummaryDTO::status,
                        Collectors.counting()
                ));
    }

    /**
     * Retrieves critical tickets created in the last 24 hours for alert dashboard.
     * Uses TicketSearchCriteria builder for complex query.
     */
    public List<TicketSummaryDTO> getCriticalTicketsLast24Hours() {
        Instant yesterday = Instant.now().minus(24, ChronoUnit.HOURS);

        TicketSearchCriteria criteria = TicketSearchCriteria.builder()
                .priority(Priority.CRITICAL)
                .createdAfter(yesterday)
                .build();

        return ticketQueryService.findByCriteria(criteria);
    }

    /**
     * Counts open tickets by priority for SLA monitoring dashboard.
     */
    public Map<Priority, Long> getOpenTicketsByPriority() {
        List<TicketSummaryDTO> newTickets = ticketQueryService.findByStatus(TicketStatus.NEW);
        List<TicketSummaryDTO> assignedTickets = ticketQueryService.findByStatus(TicketStatus.ASSIGNED);
        List<TicketSummaryDTO> inProgressTickets = ticketQueryService.findByStatus(TicketStatus.IN_PROGRESS);

        List<TicketSummaryDTO> allOpenTickets = new java.util.ArrayList<>();
        allOpenTickets.addAll(newTickets);
        allOpenTickets.addAll(assignedTickets);
        allOpenTickets.addAll(inProgressTickets);

        return allOpenTickets.stream()
                .collect(Collectors.groupingBy(
                        TicketSummaryDTO::priority,
                        Collectors.counting()
                ));
    }

    /**
     * Uses TicketSearchCriteria for complex filtered query.
     */
    public List<TicketSummaryDTO> getFilteredTickets(TicketStatus status, Priority priority, String category, UUID assigneeId) {
        TicketSearchCriteria.Builder criteriaBuilder = TicketSearchCriteria.builder();

        if (status != null) {
            criteriaBuilder.status(status);
        }
        if (priority != null) {
            criteriaBuilder.priority(priority);
        }
        if (category != null) {
            criteriaBuilder.category(category);
        }
        if (assigneeId != null) {
            criteriaBuilder.assigneeId(assigneeId);
        }

        return ticketQueryService.findByCriteria(criteriaBuilder.build());
    }
}
```

#### Key Principles for SPI Usage

1. **Use DTOs, Never Internal Entities**: Always work with `TicketSummaryDTO`, never import or reference `Ticket`, `Incident`, or `ServiceRequest` entities from `itsm.internal.domain`.

2. **Inject via Constructor**: Use constructor injection for `TicketQueryService` to enable testing with mocks/stubs.

3. **Respect SPI Stability Contract**: `TicketQueryService` is a stable API boundary. Client modules can depend on method signatures not changing. New methods may be added but existing methods will not break.

4. **Use Builder Pattern for Complex Queries**: `TicketSearchCriteria.builder()` provides fluent API for multi-criteria queries without forcing clients to provide all parameters.

5. **Handle Optional Results**: `findById()` returns `Optional<TicketSummaryDTO>` - always check `isPresent()` before accessing data.

6. **Read-Only Operations**: SPI is for queries only. Write operations (create, update, delete) must go through ITSM module's public APIs (future Story 2.3 REST endpoints) or domain events.

7. **Transactional Consistency**: SPI queries use `@Transactional(readOnly=true)` for read-committed isolation level. For real-time consistency, subscribe to domain events via eventing module.

#### Testing SPI Usage

```java
@SpringBootTest
@Testcontainers
class TaskTicketLinkServiceIT {

    @Autowired
    private TicketQueryService ticketQueryService; // Injects real implementation

    @Autowired
    private TaskTicketLinkService taskTicketLinkService;

    @Test
    void shouldBlockTaskClosureIfTicketNotResolved() {
        // Given: Create a ticket via ITSM module
        UUID ticketId = createTestTicket();

        // When: Check if task can be closed
        boolean canClose = taskTicketLinkService.canCloseTask(taskId, List.of(ticketId));

        // Then: Task closure blocked
        assertThat(canClose).isFalse();
    }
}
```

#### ModularityTests Verification

```java
@Test
void ticketQueryServiceSpiIsAccessibleFromPmModule() {
    ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);

    // Verify PM module can access ITSM SPI
    modules.getModuleByName("pm")
            .orElseThrow()
            .getDependencies(DependencyDepth.IMMEDIATE)
            .stream()
            .anyMatch(dep -> dep.getName().equals("itsm"));

    // Verify no violations
    modules.verify();
}
```

---

**Last Updated**: 2025-10-09 (Story 2.2 - Task 8.6)

