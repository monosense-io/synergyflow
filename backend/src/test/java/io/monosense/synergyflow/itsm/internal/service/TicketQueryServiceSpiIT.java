package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSearchCriteria;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static java.util.Objects.requireNonNull;

/**
 * Integration tests for TicketQueryService SPI contract.
 *
 * <p>Verifies the cross-module query interface works correctly with real PostgreSQL database
 * via Testcontainers. Tests cover all SPI methods, complex criteria queries, and validates
 * proper DTO mapping from internal entities.</p>
 *
 * @author monosense
 * @since 2.2
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TicketQueryService SPI Integration Tests")
@org.springframework.test.context.ActiveProfiles("test")
class TicketQueryServiceSpiIT {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TicketQueryService ticketQueryService;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    private UUID requester1;
    private UUID requester2;
    private UUID agent1;
    private UUID agent2;

    @BeforeEach
    void setUp() {
        // Ensure a clean slate per test (container persists across test methods)
        jdbcTemplate.execute("TRUNCATE TABLE outbox CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE tickets CASCADE");

        // Create test users in the database
        requester1 = createUser("requester1");
        requester2 = createUser("requester2");
        agent1 = createUser("agent1");
        agent2 = createUser("agent2");
    }

    @Test
    @DisplayName("IT-SPI-1: findById() returns TicketSummaryDTO for existing ticket")
    void testFindByIdReturnsTicketSummaryDTO() {
        // Given: Create a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Database connection timeout",
                "Production DB experiencing timeouts",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Database",
                requester1
        );

        var ticket = ticketService.createTicket(command, requester1);

        // When: Query by ID via SPI
        Optional<TicketSummaryDTO> result = ticketQueryService.findById(ticket.getId());

        // Then: TicketSummaryDTO returned with correct data
        assertThat(result).isPresent();
        TicketSummaryDTO dto = result.get();
        assertThat(dto.id()).isEqualTo(ticket.getId());
        assertThat(dto.title()).isEqualTo("Database connection timeout");
        assertThat(dto.description()).isEqualTo("Production DB experiencing timeouts");
        assertThat(dto.ticketType()).isEqualTo(io.monosense.synergyflow.itsm.spi.TicketType.INCIDENT);
        assertThat(dto.status()).isEqualTo(io.monosense.synergyflow.itsm.spi.TicketStatus.NEW);
        assertThat(dto.priority()).isEqualTo(io.monosense.synergyflow.itsm.spi.Priority.HIGH);
        assertThat(dto.category()).isEqualTo("Database");
        assertThat(dto.requesterId()).isEqualTo(requester1);
        assertThat(dto.assigneeId()).isNull();
        assertThat(dto.createdAt()).isNotNull();
        assertThat(dto.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("IT-SPI-1: findById() returns empty for non-existent ticket")
    void testFindByIdReturnsEmptyForNonExistent() {
        // Given: Non-existent ticket ID
        UUID nonExistentId = UUID.randomUUID();

        // When: Query by ID via SPI
        Optional<TicketSummaryDTO> result = ticketQueryService.findById(nonExistentId);

        // Then: Empty Optional returned
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("IT-SPI-1: findById() throws IllegalArgumentException for null ID")
    void testFindByIdThrowsForNullId() {
        // When/Then: Null ID throws IllegalArgumentException
        assertThatThrownBy(() -> ticketQueryService.findById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticketId cannot be null");
    }

    @Test
    @DisplayName("IT-SPI-2: findByStatus() returns filtered list of tickets by status")
    void testFindByStatusReturnsFilteredList() {
        // Given: Create 3 tickets with status NEW, 2 tickets with status ASSIGNED
        CreateTicketCommand command1 = new CreateTicketCommand(
                "Ticket 1", "Description 1", TicketType.INCIDENT, Priority.MEDIUM, "Network", requester1
        );
        CreateTicketCommand command2 = new CreateTicketCommand(
                "Ticket 2", "Description 2", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand command3 = new CreateTicketCommand(
                "Ticket 3", "Description 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        var ticket1 = ticketService.createTicket(command1, requester1); // NEW
        var ticket2 = ticketService.createTicket(command2, requester1); // NEW
        var ticket3 = ticketService.createTicket(command3, requester2); // NEW

        // Assign ticket2 and ticket3 to change status
        ticketService.assignTicket(ticket2.getId(), agent1, requester1); // ASSIGNED
        ticketService.assignTicket(ticket3.getId(), agent2, requester2); // ASSIGNED

        // When: Query by status NEW via SPI
        List<TicketSummaryDTO> newTickets = ticketQueryService.findByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus.NEW);

        // Then: Only tickets with status NEW returned
        assertThat(newTickets).hasSize(1);
        assertThat(newTickets).extracting(TicketSummaryDTO::id).containsExactly(ticket1.getId());
        assertThat(newTickets).allMatch(dto -> dto.status() == io.monosense.synergyflow.itsm.spi.TicketStatus.NEW);

        // When: Query by status ASSIGNED via SPI
        List<TicketSummaryDTO> assignedTickets = ticketQueryService.findByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus.ASSIGNED);

        // Then: Only tickets with status ASSIGNED returned
        assertThat(assignedTickets).hasSize(2);
        assertThat(assignedTickets).extracting(TicketSummaryDTO::id).containsExactlyInAnyOrder(ticket2.getId(), ticket3.getId());
        assertThat(assignedTickets).allMatch(dto -> dto.status() == io.monosense.synergyflow.itsm.spi.TicketStatus.ASSIGNED);
    }

    @Test
    @DisplayName("IT-SPI-3: findByCriteria() complex query with multiple criteria (status + priority + assignee)")
    void testFindByCriteriaWithMultipleCriteria() {
        // Given: Create multiple tickets with various combinations
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Critical Network Issue", "Desc 1", TicketType.INCIDENT, Priority.CRITICAL, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "High Priority Network Issue", "Desc 2", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Critical Database Issue", "Desc 3", TicketType.INCIDENT, Priority.CRITICAL, "Database", requester2
        );
        CreateTicketCommand cmd4 = new CreateTicketCommand(
                "Medium Priority Request", "Desc 4", TicketType.SERVICE_REQUEST, Priority.MEDIUM, "Access", requester2
        );

        var ticket1 = ticketService.createTicket(cmd1, requester1);
        var ticket2 = ticketService.createTicket(cmd2, requester1);
        var ticket3 = ticketService.createTicket(cmd3, requester2);
        var ticket4 = ticketService.createTicket(cmd4, requester2);

        // Assign tickets to different agents
        ticketService.assignTicket(ticket1.getId(), agent1, requester1); // ASSIGNED, CRITICAL, Network, agent1
        ticketService.assignTicket(ticket2.getId(), agent1, requester1); // ASSIGNED, HIGH, Network, agent1
        ticketService.assignTicket(ticket3.getId(), agent2, requester2); // ASSIGNED, CRITICAL, Database, agent2
        ticketService.assignTicket(ticket4.getId(), agent2, requester2); // ASSIGNED, MEDIUM, Access, agent2

        // When: Build complex criteria (status=ASSIGNED, priority=CRITICAL, assigneeId=agent1)
        TicketSearchCriteria criteria = TicketSearchCriteria.builder()
                .status(io.monosense.synergyflow.itsm.spi.TicketStatus.ASSIGNED)
                .priority(io.monosense.synergyflow.itsm.spi.Priority.CRITICAL)
                .assigneeId(agent1)
                .build();

        List<TicketSummaryDTO> results = ticketQueryService.findByCriteria(criteria);

        // Then: Only ticket1 matches ALL criteria
        assertThat(results).hasSize(1);
        assertThat(results.get(0).id()).isEqualTo(ticket1.getId());
        assertThat(results.get(0).status()).isEqualTo(io.monosense.synergyflow.itsm.spi.TicketStatus.ASSIGNED);
        assertThat(results.get(0).priority()).isEqualTo(io.monosense.synergyflow.itsm.spi.Priority.CRITICAL);
        assertThat(results.get(0).assigneeId()).isEqualTo(agent1);
        assertThat(results.get(0).category()).isEqualTo("Network");
    }

    @Test
    @DisplayName("IT-SPI-3: findByCriteria() with category filter")
    void testFindByCriteriaWithCategoryFilter() {
        // Given: Create tickets with different categories
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Network Issue 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Network Issue 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Network", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Database Issue", "Desc 3", TicketType.INCIDENT, Priority.HIGH, "Database", requester2
        );

        ticketService.createTicket(cmd1, requester1);
        ticketService.createTicket(cmd2, requester1);
        ticketService.createTicket(cmd3, requester2);

        // When: Query by category "Network"
        TicketSearchCriteria criteria = TicketSearchCriteria.builder()
                .category("Network")
                .build();

        List<TicketSummaryDTO> results = ticketQueryService.findByCriteria(criteria);

        // Then: Only tickets with category "Network" returned
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(dto -> "Network".equals(dto.category()));
    }

    @Test
    @DisplayName("IT-SPI-3: findByCriteria() with empty criteria returns all tickets")
    void testFindByCriteriaWithEmptyCriteria() {
        // Given: Create 3 tickets
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Ticket 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Ticket 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Ticket 3", "Desc 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        ticketService.createTicket(cmd1, requester1);
        ticketService.createTicket(cmd2, requester1);
        ticketService.createTicket(cmd3, requester2);

        // When: Query with empty criteria
        TicketSearchCriteria criteria = TicketSearchCriteria.builder().build();

        List<TicketSummaryDTO> results = ticketQueryService.findByCriteria(criteria);

        // Then: All tickets returned (at least the 3 we created)
        assertThat(results.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("IT-SPI-4: SPI interface is injectable (cross-module access verification)")
    void testSpiInterfaceIsInjectable() {
        // Given/When: TicketQueryService injected via Spring
        // Then: Bean injection successful (verified by @Autowired setup)
        assertThat(ticketQueryService).isNotNull();
        assertThat(ticketQueryService).isInstanceOf(TicketQueryService.class);

        // Verify it's the implementation class (package-private)
        assertThat(ticketQueryService.getClass().getName())
                .startsWith("io.monosense.synergyflow.itsm.internal.service.TicketQueryServiceImpl");
    }

    @Test
    @DisplayName("IT-SPI-2: findByPriority() returns filtered list of tickets by priority")
    void testFindByPriorityReturnsFilteredList() {
        // Given: Create tickets with different priorities
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Critical Issue", "Desc 1", TicketType.INCIDENT, Priority.CRITICAL, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "High Priority Issue", "Desc 2", TicketType.INCIDENT, Priority.HIGH, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Another Critical Issue", "Desc 3", TicketType.INCIDENT, Priority.CRITICAL, "Network", requester2
        );

        ticketService.createTicket(cmd1, requester1);
        ticketService.createTicket(cmd2, requester1);
        ticketService.createTicket(cmd3, requester2);

        // When: Query by priority CRITICAL via SPI
        List<TicketSummaryDTO> criticalTickets = ticketQueryService.findByPriority(io.monosense.synergyflow.itsm.spi.Priority.CRITICAL);

        // Then: Only tickets with priority CRITICAL returned
        assertThat(criticalTickets).hasSize(2);
        assertThat(criticalTickets).allMatch(dto -> dto.priority() == io.monosense.synergyflow.itsm.spi.Priority.CRITICAL);
    }

    @Test
    @DisplayName("IT-SPI-2: findByAssignee() returns filtered list of tickets by assignee")
    void testFindByAssigneeReturnsFilteredList() {
        // Given: Create tickets and assign to different agents
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Ticket 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Ticket 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Ticket 3", "Desc 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        var ticket1 = ticketService.createTicket(cmd1, requester1);
        var ticket2 = ticketService.createTicket(cmd2, requester1);
        var ticket3 = ticketService.createTicket(cmd3, requester2);

        // Assign ticket1 and ticket2 to agent1, ticket3 to agent2
        ticketService.assignTicket(ticket1.getId(), agent1, requester1);
        ticketService.assignTicket(ticket2.getId(), agent1, requester1);
        ticketService.assignTicket(ticket3.getId(), agent2, requester2);

        // When: Query by assignee agent1 via SPI
        List<TicketSummaryDTO> agent1Tickets = ticketQueryService.findByAssignee(agent1);

        // Then: Only tickets assigned to agent1 returned
        assertThat(agent1Tickets).hasSize(2);
        assertThat(agent1Tickets).extracting(TicketSummaryDTO::id).containsExactlyInAnyOrder(ticket1.getId(), ticket2.getId());
        assertThat(agent1Tickets).allMatch(dto -> agent1.equals(dto.assigneeId()));
    }

    @Test
    @DisplayName("IT-SPI-2: findByRequester() returns filtered list of tickets by requester")
    void testFindByRequesterReturnsFilteredList() {
        // Given: Create tickets by different requesters
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Ticket 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Ticket 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Ticket 3", "Desc 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        var ticket1 = ticketService.createTicket(cmd1, requester1);
        var ticket2 = ticketService.createTicket(cmd2, requester1);
        var ticket3 = ticketService.createTicket(cmd3, requester2);

        // When: Query by requester requester1 via SPI
        List<TicketSummaryDTO> requester1Tickets = ticketQueryService.findByRequester(requester1);

        // Then: Only tickets created by requester1 returned
        assertThat(requester1Tickets).hasSize(2);
        assertThat(requester1Tickets).extracting(TicketSummaryDTO::id).containsExactlyInAnyOrder(ticket1.getId(), ticket2.getId());
        assertThat(requester1Tickets).allMatch(dto -> requester1.equals(dto.requesterId()));
    }

    @Test
    @DisplayName("IT-SPI-2: existsById() returns true for existing ticket")
    void testExistsByIdReturnsTrueForExistingTicket() {
        // Given: Create a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Ticket", "Description", TicketType.INCIDENT, Priority.MEDIUM, "Network", requester1
        );
        var ticket = ticketService.createTicket(command, requester1);

        // When: Check existence via SPI
        boolean exists = ticketQueryService.existsById(ticket.getId());

        // Then: Returns true
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("IT-SPI-2: existsById() returns false for non-existent ticket")
    void testExistsByIdReturnsFalseForNonExistent() {
        // Given: Non-existent ticket ID
        UUID nonExistentId = UUID.randomUUID();

        // When: Check existence via SPI
        boolean exists = ticketQueryService.existsById(nonExistentId);

        // Then: Returns false
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("IT-SPI-2: countByStatus() returns correct count")
    void testCountByStatusReturnsCorrectCount() {
        // Given: Create tickets with different statuses
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Ticket 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Ticket 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Ticket 3", "Desc 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        ticketService.createTicket(cmd1, requester1); // NEW
        var ticket2 = ticketService.createTicket(cmd2, requester1); // NEW
        ticketService.createTicket(cmd3, requester2); // NEW

        // Assign ticket2 to change status
        ticketService.assignTicket(ticket2.getId(), agent1, requester1); // ASSIGNED

        // When: Count by status via SPI
        long newCount = ticketQueryService.countByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus.NEW);
        long assignedCount = ticketQueryService.countByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus.ASSIGNED);

        // Then: Correct counts returned
        assertThat(newCount).isGreaterThanOrEqualTo(2); // At least ticket1 and ticket3
        assertThat(assignedCount).isGreaterThanOrEqualTo(1); // At least ticket2
    }

    @Test
    @DisplayName("IT-SPI-2: countByAssignee() returns correct count")
    void testCountByAssigneeReturnsCorrectCount() {
        // Given: Create tickets and assign to agents
        CreateTicketCommand cmd1 = new CreateTicketCommand(
                "Ticket 1", "Desc 1", TicketType.INCIDENT, Priority.HIGH, "Network", requester1
        );
        CreateTicketCommand cmd2 = new CreateTicketCommand(
                "Ticket 2", "Desc 2", TicketType.INCIDENT, Priority.MEDIUM, "Database", requester1
        );
        CreateTicketCommand cmd3 = new CreateTicketCommand(
                "Ticket 3", "Desc 3", TicketType.SERVICE_REQUEST, Priority.LOW, "Access", requester2
        );

        var ticket1 = ticketService.createTicket(cmd1, requester1);
        var ticket2 = ticketService.createTicket(cmd2, requester1);
        var ticket3 = ticketService.createTicket(cmd3, requester2);

        // Assign tickets
        ticketService.assignTicket(ticket1.getId(), agent1, requester1);
        ticketService.assignTicket(ticket2.getId(), agent1, requester1);
        ticketService.assignTicket(ticket3.getId(), agent2, requester2);

        // When: Count by assignee via SPI
        long agent1Count = ticketQueryService.countByAssignee(agent1);
        long agent2Count = ticketQueryService.countByAssignee(agent2);

        // Then: Correct counts returned
        assertThat(agent1Count).isGreaterThanOrEqualTo(2); // At least ticket1 and ticket2
        assertThat(agent2Count).isGreaterThanOrEqualTo(1); // At least ticket3
    }

    @Test
    @DisplayName("IT-CACHE-1: findById() uses cache - verify cache hits and misses")
    void testFindByIdUsesCache() {
        // Given: Create a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Cache Ticket",
                "Description for cache testing",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Network",
                requester1
        );
        var ticket = ticketService.createTicket(command, requester1);
        UUID ticketId = ticket.getId();

        // Get the tickets cache
        Cache ticketsCache = requireNonNull(cacheManager.getCache("tickets"));

        // Verify cache is initially empty for this ticket
        assertThat(ticketsCache.get(ticketId)).isNull();

        // When: First call to findById (cache MISS - should hit database)
        Optional<TicketSummaryDTO> firstResult = ticketQueryService.findById(ticketId);

        // Then: Result is present and cache now contains the value
        assertThat(firstResult).isPresent();
        assertThat(firstResult.get().id()).isEqualTo(ticketId);
        assertThat(firstResult.get().title()).isEqualTo("Test Cache Ticket");

        // Verify value is now in cache (Spring caches the unwrapped TicketSummaryDTO, not the Optional)
        Cache.ValueWrapper cachedValue = requireNonNull(ticketsCache.get(ticketId));
        Object cachedRaw = requireNonNull(cachedValue.get());
        assertThat(cachedRaw).isInstanceOf(TicketSummaryDTO.class);
        TicketSummaryDTO cachedDTO = (TicketSummaryDTO) cachedRaw;
        assertThat(cachedDTO.id()).isEqualTo(ticketId);
        assertThat(cachedDTO.title()).isEqualTo("Test Cache Ticket");

        // When: Second call to findById with same ID (cache HIT - should NOT hit database)
        Optional<TicketSummaryDTO> secondResult = ticketQueryService.findById(ticketId);

        // Then: Same result returned from cache
        assertThat(secondResult).isPresent();
        assertThat(secondResult.get().id()).isEqualTo(ticketId);
        assertThat(secondResult.get().title()).isEqualTo("Test Cache Ticket");

        // Verify both results are equal (same data)
        assertThat(firstResult.get()).isEqualTo(secondResult.get());
    }

    @Test
    @DisplayName("IT-CACHE-2: findById() returns empty for non-existent ticket - cached as null")
    void testFindByIdEmptyResultCachedAsNull() {
        // Given: Non-existent ticket ID
        UUID nonExistentId = UUID.randomUUID();

        // Get the tickets cache
        Cache ticketsCache = requireNonNull(cacheManager.getCache("tickets"));

        // When: First call to findById with non-existent ID (cache MISS)
        Optional<TicketSummaryDTO> firstResult = ticketQueryService.findById(nonExistentId);

        // Then: Result is empty
        assertThat(firstResult).isEmpty();

        // Verify empty result IS cached as null (Spring unwraps Optional.empty() to null and caches it)
        Cache.ValueWrapper cachedValue = requireNonNull(ticketsCache.get(nonExistentId));
        Object cached = cachedValue.get();
        assertThat(cached).isNull();  // Spring caches null for Optional.empty()

        // When: Second call to findById with same non-existent ID (cache HIT)
        Optional<TicketSummaryDTO> secondResult = ticketQueryService.findById(nonExistentId);

        // Then: Still empty result (from cache this time)
        assertThat(secondResult).isEmpty();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Creates a test user and returns the UUID.
     */
    private UUID createUser(String username) {
        UUID userId = UUID.randomUUID();
        String uniqueUsername = username + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, username + " User", true, 1
        );
        return userId;
    }
}
