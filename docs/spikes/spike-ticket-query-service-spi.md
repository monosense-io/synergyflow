# SPIKE: TicketQueryService SPI Contract Design

**Date:** 2025-10-09
**Owner:** Winston (Architect)
**Story:** Story 2.2 Preparation Sprint
**Status:** Ready for Implementation
**Estimated Effort:** 2 hours

---

## Objective

Define the Service Provider Interface (SPI) for cross-module ticket queries, enabling other modules (PM, Workflow, Dashboard) to query ticket data without violating Spring Modulith module boundaries.

---

## Problem Statement

From Spring Modulith architecture (module-boundaries.md):
- **ITSM internal packages** (`internal/domain/`, `internal/repository/`, `internal/service/`) are **implementation details**
- **Other modules** (PM, Workflow, Dashboard) need to query ticket data for their features:
  - **PM Module**: Link tasks to tickets, display ticket status in project views
  - **Workflow Module**: Trigger workflows when ticket state changes
  - **Dashboard Module**: Aggregate ticket metrics across modules

**Challenge:** How do other modules query tickets without accessing ITSM internal repositories directly (which violates modularity)?

**Solution:** Define **TicketQueryService** as a public SPI interface in `itsm/spi/` package, implemented by ITSM internal service layer.

---

## Spring Modulith SPI Pattern

### Module Boundary Rules

```
ITSM Module Structure:
└── io.monosense.synergyflow.itsm
    ├── api/                   (PUBLIC - REST controllers)
    ├── spi/                   (PUBLIC - @NamedInterface for cross-module)
    │   └── TicketQueryService.java  ← SPI CONTRACT
    ├── events/                (PUBLIC - domain events)
    └── internal/              (INTERNAL - implementation details)
        ├── domain/            (entities - package-private)
        ├── repository/        (JPA repos - package-private)
        └── service/           (business logic - package-private)
            └── TicketQueryServiceImpl.java  ← SPI IMPLEMENTATION
```

### SPI Contract Rules

1. **Location**: `spi/` package (public access, cross-module boundary)
2. **Annotation**: `@org.springframework.modulith.NamedInterface` (marks as cross-module API)
3. **Return Types**: DTOs (Data Transfer Objects), never entities
4. **Dependencies**: SPI interface has ZERO dependencies on internal packages
5. **Versioning**: SPI is a public API contract - breaking changes require deprecation cycle

---

## TicketQueryService SPI Contract

### Interface Definition

```java
package io.monosense.synergyflow.itsm.spi;

import org.springframework.modulith.NamedInterface;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Provider Interface for cross-module ticket queries.
 *
 * <p>This interface is part of the ITSM module's public SPI, allowing other modules
 * (PM, Workflow, Dashboard) to query ticket data without accessing internal repositories.
 *
 * <p><b>Modularity Contract:</b> This interface is a stable API boundary. Breaking changes
 * require deprecation cycle and version migration.
 *
 * @since 2.2
 */
@NamedInterface("TicketQueryService")
public interface TicketQueryService {

    /**
     * Find ticket by ID.
     *
     * @param ticketId the ticket ID
     * @return ticket summary DTO, or empty if not found
     */
    Optional<TicketSummaryDTO> findById(UUID ticketId);

    /**
     * Find all tickets assigned to a specific user.
     *
     * @param assigneeId the assignee user ID
     * @return list of ticket summaries (empty if none found)
     */
    List<TicketSummaryDTO> findByAssignee(UUID assigneeId);

    /**
     * Find all tickets created by a specific requester.
     *
     * @param requesterId the requester user ID
     * @return list of ticket summaries (empty if none found)
     */
    List<TicketSummaryDTO> findByRequester(UUID requesterId);

    /**
     * Find all tickets with a specific status.
     *
     * @param status the ticket status
     * @return list of ticket summaries (empty if none found)
     */
    List<TicketSummaryDTO> findByStatus(TicketStatus status);

    /**
     * Find all tickets with a specific priority.
     *
     * @param priority the priority level
     * @return list of ticket summaries (empty if none found)
     */
    List<TicketSummaryDTO> findByPriority(Priority priority);

    /**
     * Find tickets by multiple criteria (AND logic).
     *
     * @param criteria the search criteria
     * @return list of ticket summaries matching all criteria
     */
    List<TicketSummaryDTO> findByCriteria(TicketSearchCriteria criteria);

    /**
     * Check if a ticket exists.
     *
     * @param ticketId the ticket ID
     * @return true if ticket exists, false otherwise
     */
    boolean existsById(UUID ticketId);

    /**
     * Count tickets by status.
     *
     * @param status the ticket status
     * @return count of tickets with given status
     */
    long countByStatus(TicketStatus status);

    /**
     * Count tickets by assignee.
     *
     * @param assigneeId the assignee user ID
     * @return count of tickets assigned to user
     */
    long countByAssignee(UUID assigneeId);
}
```

---

## Data Transfer Objects (DTOs)

### TicketSummaryDTO

```java
package io.monosense.synergyflow.itsm.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight ticket summary for cross-module queries.
 *
 * <p>This DTO exposes only essential ticket data needed by other modules,
 * hiding internal entity structure.
 *
 * @since 2.2
 */
public record TicketSummaryDTO(
    UUID id,
    TicketType ticketType,
    String title,
    String description,
    TicketStatus status,
    Priority priority,
    String category,
    UUID requesterId,
    UUID assigneeId,
    Instant createdAt,
    Instant updatedAt
) {
    /**
     * Convert from Ticket entity to DTO.
     */
    public static TicketSummaryDTO from(Ticket ticket) {
        return new TicketSummaryDTO(
            ticket.getId(),
            ticket.getTicketType(),
            ticket.getTitle(),
            ticket.getDescription(),
            ticket.getStatus(),
            ticket.getPriority(),
            ticket.getCategory(),
            ticket.getRequesterId(),
            ticket.getAssigneeId(),
            ticket.getCreatedAt(),
            ticket.getUpdatedAt()
        );
    }
}
```

### TicketSearchCriteria

```java
package io.monosense.synergyflow.itsm.spi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Search criteria for multi-field ticket queries.
 *
 * <p>All fields are optional. Null fields are ignored (no filter applied).
 *
 * @since 2.2
 */
public record TicketSearchCriteria(
    Optional<TicketStatus> status,
    Optional<Priority> priority,
    Optional<String> category,
    Optional<UUID> assigneeId,
    Optional<UUID> requesterId,
    Optional<Instant> createdAfter,
    Optional<Instant> createdBefore
) {
    /**
     * Builder for fluent criteria construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Optional<TicketStatus> status = Optional.empty();
        private Optional<Priority> priority = Optional.empty();
        private Optional<String> category = Optional.empty();
        private Optional<UUID> assigneeId = Optional.empty();
        private Optional<UUID> requesterId = Optional.empty();
        private Optional<Instant> createdAfter = Optional.empty();
        private Optional<Instant> createdBefore = Optional.empty();

        public Builder status(TicketStatus status) {
            this.status = Optional.of(status);
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = Optional.of(priority);
            return this;
        }

        public Builder category(String category) {
            this.category = Optional.of(category);
            return this;
        }

        public Builder assigneeId(UUID assigneeId) {
            this.assigneeId = Optional.of(assigneeId);
            return this;
        }

        public Builder requesterId(UUID requesterId) {
            this.requesterId = Optional.of(requesterId);
            return this;
        }

        public Builder createdAfter(Instant createdAfter) {
            this.createdAfter = Optional.of(createdAfter);
            return this;
        }

        public Builder createdBefore(Instant createdBefore) {
            this.createdBefore = Optional.of(createdBefore);
            return this;
        }

        public TicketSearchCriteria build() {
            return new TicketSearchCriteria(
                status, priority, category, assigneeId, requesterId, createdAfter, createdBefore
            );
        }
    }
}
```

### Enums (Public API)

```java
package io.monosense.synergyflow.itsm.spi;

// Copy of internal enums for SPI boundary (prevents coupling to internal package)

public enum TicketStatus {
    NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
}

public enum TicketType {
    INCIDENT, SERVICE_REQUEST
}

public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW
}
```

---

## SPI Implementation

### TicketQueryServiceImpl (Internal)

```java
package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.spi.*;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TicketQueryService SPI.
 *
 * <p>This internal service implements the public SPI contract, translating
 * SPI DTOs to/from internal domain entities.
 *
 * @since 2.2
 */
@Service
@Transactional(readOnly = true)
class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;

    TicketQueryServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Optional<TicketSummaryDTO> findById(UUID ticketId) {
        return ticketRepository.findById(ticketId)
            .map(TicketSummaryDTO::from);
    }

    @Override
    public List<TicketSummaryDTO> findByAssignee(UUID assigneeId) {
        return ticketRepository.findByAssigneeId(assigneeId).stream()
            .map(TicketSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<TicketSummaryDTO> findByRequester(UUID requesterId) {
        return ticketRepository.findByRequesterId(requesterId).stream()
            .map(TicketSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<TicketSummaryDTO> findByStatus(TicketStatus status) {
        // Convert SPI enum to internal enum
        io.monosense.synergyflow.itsm.internal.domain.TicketStatus internalStatus =
            io.monosense.synergyflow.itsm.internal.domain.TicketStatus.valueOf(status.name());

        return ticketRepository.findByStatus(internalStatus).stream()
            .map(TicketSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<TicketSummaryDTO> findByPriority(Priority priority) {
        // Convert SPI enum to internal enum
        io.monosense.synergyflow.itsm.internal.domain.Priority internalPriority =
            io.monosense.synergyflow.itsm.internal.domain.Priority.valueOf(priority.name());

        return ticketRepository.findByPriority(internalPriority).stream()
            .map(TicketSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Override
    public List<TicketSummaryDTO> findByCriteria(TicketSearchCriteria criteria) {
        // Use Spring Data JPA Specification for dynamic queries
        return ticketRepository.findAll(TicketSpecifications.fromCriteria(criteria)).stream()
            .map(TicketSummaryDTO::from)
            .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID ticketId) {
        return ticketRepository.existsById(ticketId);
    }

    @Override
    public long countByStatus(TicketStatus status) {
        io.monosense.synergyflow.itsm.internal.domain.TicketStatus internalStatus =
            io.monosense.synergyflow.itsm.internal.domain.TicketStatus.valueOf(status.name());

        return ticketRepository.countByStatus(internalStatus);
    }

    @Override
    public long countByAssignee(UUID assigneeId) {
        return ticketRepository.countByAssigneeId(assigneeId);
    }
}
```

---

## Cross-Module Usage Examples

### Example 1: PM Module - Link Task to Ticket

```java
package io.monosense.synergyflow.pm.internal.service;

import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;

@Service
class TaskService {

    private final TicketQueryService ticketQueryService; // Injected from ITSM module

    public Task linkTicketToTask(UUID taskId, UUID ticketId) {
        // Query ticket via SPI (no direct access to ITSM internal repository)
        TicketSummaryDTO ticket = ticketQueryService.findById(ticketId)
            .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate ticket status before linking
        if (ticket.status() != TicketStatus.RESOLVED && ticket.status() != TicketStatus.CLOSED) {
            throw new InvalidLinkException("Can only link resolved/closed tickets to tasks");
        }

        Task task = taskRepository.findById(taskId).orElseThrow();
        task.linkTicket(ticketId);
        return taskRepository.save(task);
    }
}
```

### Example 2: Dashboard Module - Aggregate Ticket Metrics

```java
package io.monosense.synergyflow.dashboard.internal.service;

import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketStatus;

@Service
class MetricsAggregationService {

    private final TicketQueryService ticketQueryService;

    public DashboardMetrics getOverviewMetrics() {
        long openTickets = ticketQueryService.countByStatus(TicketStatus.NEW)
            + ticketQueryService.countByStatus(TicketStatus.ASSIGNED)
            + ticketQueryService.countByStatus(TicketStatus.IN_PROGRESS);

        long resolvedTickets = ticketQueryService.countByStatus(TicketStatus.RESOLVED);
        long closedTickets = ticketQueryService.countByStatus(TicketStatus.CLOSED);

        return new DashboardMetrics(openTickets, resolvedTickets, closedTickets);
    }
}
```

### Example 3: Workflow Module - Trigger on Ticket State Change

```java
package io.monosense.synergyflow.workflow.internal;

import io.monosense.synergyflow.itsm.events.TicketStateChangedEvent;
import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import org.springframework.modulith.events.ApplicationModuleListener;

@Service
class WorkflowTriggerService {

    private final TicketQueryService ticketQueryService;

    @ApplicationModuleListener
    void onTicketStateChanged(TicketStateChangedEvent event) {
        // Fetch ticket details via SPI
        TicketSummaryDTO ticket = ticketQueryService.findById(event.ticketId())
            .orElse(null);

        if (ticket != null && ticket.status() == TicketStatus.RESOLVED) {
            // Trigger "Post-Resolution Survey" workflow
            workflowEngine.triggerWorkflow("post-resolution-survey", ticket.requesterId());
        }
    }
}
```

---

## Performance Considerations

### Read-Only Queries

- **All SPI methods are read-only** → Annotate implementation with `@Transactional(readOnly = true)`
- **Benefits**: Read-only transactions allow database query optimization (no dirty checking)

### Caching Strategy

For frequently accessed queries:

```java
@Service
@Transactional(readOnly = true)
class TicketQueryServiceImpl implements TicketQueryService {

    @Override
    @Cacheable(value = "ticketCache", key = "#ticketId")
    public Optional<TicketSummaryDTO> findById(UUID ticketId) {
        return ticketRepository.findById(ticketId)
            .map(TicketSummaryDTO::from);
    }

    @CacheEvict(value = "ticketCache", key = "#ticketId")
    public void evictCache(UUID ticketId) {
        // Called by TicketService on ticket update
    }
}
```

**Cache Configuration:**

```yaml
# application.yml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=5m
```

### Pagination Support (Future Enhancement)

For large result sets, consider adding pagination:

```java
List<TicketSummaryDTO> findByCriteria(TicketSearchCriteria criteria, Pageable pageable);
```

---

## Testing Strategy

### SPI Contract Tests

```java
@SpringBootTest
@Transactional
class TicketQueryServiceSpiTest {

    @Autowired
    private TicketQueryService ticketQueryService; // Injected SPI implementation

    @Test
    void findById_existingTicket_returnsDTO() {
        // Given: Ticket exists in database
        Ticket ticket = createAndSaveTicket();

        // When: Query via SPI
        Optional<TicketSummaryDTO> result = ticketQueryService.findById(ticket.getId());

        // Then: DTO returned with correct data
        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(ticket.getId());
        assertThat(result.get().title()).isEqualTo(ticket.getTitle());
    }

    @Test
    void findByStatus_multipleTickets_returnsFiltered() {
        // Given: 3 NEW tickets, 2 ASSIGNED tickets
        createTicketsWithStatus(TicketStatus.NEW, 3);
        createTicketsWithStatus(TicketStatus.ASSIGNED, 2);

        // When: Query NEW tickets via SPI
        List<TicketSummaryDTO> results = ticketQueryService.findByStatus(TicketStatus.NEW);

        // Then: Only NEW tickets returned
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(dto -> dto.status() == TicketStatus.NEW);
    }

    @Test
    void findByCriteria_complexQuery_returnsMatching() {
        // Given: Tickets with various criteria
        createTicket(TicketStatus.ASSIGNED, Priority.HIGH, "Network", assignee1);
        createTicket(TicketStatus.ASSIGNED, Priority.LOW, "Network", assignee1);
        createTicket(TicketStatus.IN_PROGRESS, Priority.HIGH, "Software", assignee2);

        // When: Query with multiple criteria
        TicketSearchCriteria criteria = TicketSearchCriteria.builder()
            .status(TicketStatus.ASSIGNED)
            .priority(Priority.HIGH)
            .category("Network")
            .assigneeId(assignee1)
            .build();

        List<TicketSummaryDTO> results = ticketQueryService.findByCriteria(criteria);

        // Then: Only ticket matching ALL criteria returned
        assertThat(results).hasSize(1);
    }
}
```

### Cross-Module Integration Tests

```java
@SpringBootTest
@Tag("integration")
class CrossModuleQueryTest {

    @Autowired
    private TicketQueryService ticketQueryService; // ITSM SPI

    @Autowired
    private TaskService taskService; // PM Module

    @Test
    void pmModule_canQueryTicketsViaSpi() {
        // Given: Ticket created in ITSM module
        Ticket ticket = createAndSaveTicket();

        // When: PM module queries ticket via SPI
        Optional<TicketSummaryDTO> result = ticketQueryService.findById(ticket.getId());

        // Then: PM module receives ticket data without accessing ITSM internals
        assertThat(result).isPresent();
    }
}
```

---

## Story 2.2 Acceptance Criteria

Based on this spike, Story 2.2 must include:

- ✅ **AC-SPI-1**: Create `spi/` package in ITSM module
- ✅ **AC-SPI-2**: Define `TicketQueryService` interface with @NamedInterface annotation
- ✅ **AC-SPI-3**: Create `TicketSummaryDTO` record for cross-module data transfer
- ✅ **AC-SPI-4**: Create `TicketSearchCriteria` record with builder pattern
- ✅ **AC-SPI-5**: Implement `TicketQueryServiceImpl` in `internal/service/` package
- ✅ **AC-SPI-6**: Add SPI contract tests verifying all query methods
- ✅ **AC-SPI-7**: Verify Spring Modulith allows cross-module SPI access (ModularityTests)
- ✅ **AC-SPI-8**: Document SPI usage examples in `docs/architecture/module-boundaries.md`

---

## Module Boundaries Validation

Spring Modulith will enforce:

```java
@Test
void spiInterfaceAccessibleFromOtherModules() {
    // ALLOWED: PM module can import itsm.spi.TicketQueryService
    ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);
    modules.verify(); // Should pass
}

@Test
void internalPackageNotAccessibleFromOtherModules() {
    // FORBIDDEN: PM module cannot import itsm.internal.repository.TicketRepository
    // ModularityTests will fail if PM module imports internal packages
}
```

---

## Questions for Team Review

1. **DTO Richness**: Should `TicketSummaryDTO` include subclass fields (Incident.severity, ServiceRequest.approvalStatus), or keep it minimal?
2. **Caching**: Should SPI queries be cached by default, or enable caching per-method?
3. **Pagination**: Should Story 2.2 include pagination support, or defer to Story 2.6 (Agent Console)?
4. **Event-Driven Sync**: Should PM/Workflow modules cache ticket data via events, or always query SPI?

---

**SPIKE STATUS**: ✅ COMPLETE - Ready for Story 2.2 Implementation

**Next Steps:**
1. Create `spi/` package and `TicketQueryService.java` interface
2. Implement `TicketQueryServiceImpl.java` in Story 2.2 Task 3 (after TicketService)
3. Add SPI contract tests
4. Update `module-boundaries.md` with SPI usage examples
