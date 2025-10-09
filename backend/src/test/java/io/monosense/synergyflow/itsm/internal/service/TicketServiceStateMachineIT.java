package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Incident;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException;
import io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException;
import io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for TicketService state machine (Task 7.1-7.3).
 *
 * <p>Tests verify the ticket state machine with all valid and invalid transitions,
 * authorization checks, and required field validation. Uses Testcontainers PostgreSQL
 * for realistic database state persistence testing.</p>
 *
 * <p>Covers AC-20 (IT-SM-1 to IT-SM-5):
 * <ul>
 *   <li>IT-SM-1: Full lifecycle (NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED)</li>
 *   <li>IT-SM-2: Reopen flow (RESOLVED → NEW, CLOSED → NEW with reopenCount)</li>
 *   <li>IT-SM-3: Invalid transitions throw InvalidStateTransitionException</li>
 *   <li>IT-SM-4: Authorization checks (startWork/resolve by non-assignee)</li>
 *   <li>IT-SM-5: Required field validation (assigneeId, resolutionNotes, reopenReason)</li>
 * </ul>
 *
 * @since 2.2
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TicketService State Machine Integration Tests (Task 7.1-7.3)")
class TicketServiceStateMachineIT {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceStateMachineIT.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID requesterId;
    private UUID agentId;
    private UUID otherUserId;

    @BeforeEach
    void setUp() {
        // Create test users
        requesterId = createUser("requester");
        agentId = createUser("agent");
        otherUserId = createUser("otherUser");
    }

    @Test
    @DisplayName("IT-SM-1: Full lifecycle - NEW → ASSIGNED → IN_PROGRESS → RESOLVED → CLOSED (AC-20)")
    void fullLifecycle_shouldTransitionThroughAllStates_andPublishEvents() {
        // Given: Create a ticket in NEW status
        CreateTicketCommand command = new CreateTicketCommand(
                "Critical database outage",
                "Production database is down, affecting all users",
                TicketType.INCIDENT,
                Priority.CRITICAL,
                "Database",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        log.info("Created ticket {} in status NEW", ticketId);

        // Verify initial state
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(ticket.getAssigneeId()).isNull();

        // Count initial events
        int initialEventCount = countEventsForTicket(ticketId);
        assertThat(initialEventCount).isGreaterThan(0); // TicketCreated event

        // When: Transition NEW → ASSIGNED
        Ticket assignedTicket = ticketService.assignTicket(ticketId, agentId, requesterId);
        log.info("Assigned ticket {} to agent {}, status: {}", ticketId, agentId, assignedTicket.getStatus());

        // Then: Verify ASSIGNED state persisted
        Ticket reloadedAfterAssign = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterAssign.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(reloadedAfterAssign.getAssigneeId()).isEqualTo(agentId);
        assertThat(reloadedAfterAssign.getVersion()).isGreaterThan(ticket.getVersion());

        // Verify TicketAssigned event published
        int eventsAfterAssign = countEventsForTicket(ticketId);
        assertThat(eventsAfterAssign).isGreaterThan(initialEventCount);

        // When: Transition ASSIGNED → IN_PROGRESS
        Ticket inProgressTicket = ticketService.startWork(ticketId, agentId);
        log.info("Started work on ticket {}, status: {}", ticketId, inProgressTicket.getStatus());

        // Then: Verify IN_PROGRESS state persisted
        Ticket reloadedAfterStart = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterStart.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(reloadedAfterStart.getVersion()).isGreaterThan(reloadedAfterAssign.getVersion());

        // Verify TicketStateChanged event published
        int eventsAfterStart = countEventsForTicket(ticketId);
        assertThat(eventsAfterStart).isGreaterThan(eventsAfterAssign);

        // When: Transition IN_PROGRESS → RESOLVED
        String resolutionNotes = "Database server restarted and connections restored. Root cause identified.";
        Ticket resolvedTicket = ticketService.resolveTicket(ticketId, resolutionNotes, agentId);
        log.info("Resolved ticket {}, status: {}", ticketId, resolvedTicket.getStatus());

        // Then: Verify RESOLVED state persisted
        Ticket reloadedAfterResolve = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterResolve.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(reloadedAfterResolve.getVersion()).isGreaterThan(reloadedAfterStart.getVersion());

        // For Incident, verify resolvedAt timestamp set
        if (reloadedAfterResolve instanceof Incident incident) {
            assertThat(incident.getResolvedAt()).isNotNull();
            assertThat(incident.getResolutionNotes()).isEqualTo(resolutionNotes);
        }

        // Verify TicketStateChanged event published
        int eventsAfterResolve = countEventsForTicket(ticketId);
        assertThat(eventsAfterResolve).isGreaterThan(eventsAfterStart);

        // When: Transition RESOLVED → CLOSED
        Ticket closedTicket = ticketService.closeTicket(ticketId, requesterId);
        log.info("Closed ticket {}, status: {}", ticketId, closedTicket.getStatus());

        // Then: Verify CLOSED state persisted
        Ticket reloadedAfterClose = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterClose.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(reloadedAfterClose.getVersion()).isGreaterThan(reloadedAfterResolve.getVersion());

        // Verify TicketStateChanged event published
        int finalEventCount = countEventsForTicket(ticketId);
        assertThat(finalEventCount).isGreaterThan(eventsAfterResolve);

        log.info("Test passed: Full lifecycle completed with {} state transitions and {} events published",
                4, finalEventCount - initialEventCount);
    }

    @Test
    @DisplayName("IT-SM-2: Reopen flow - RESOLVED → NEW and CLOSED → NEW with reopenCount increment (AC-20)")
    void reopenFlow_shouldTransitionToNew_andIncrementReopenCount() {
        // Given: Create and resolve a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Network connectivity issue",
                "Users reporting intermittent connectivity",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Network",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        ticketService.assignTicket(ticketId, agentId, requesterId);
        ticketService.startWork(ticketId, agentId);
        ticketService.resolveTicket(ticketId, "Network issue resolved by router restart", agentId);

        // Verify initial reopenCount is 0
        Ticket resolvedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(resolvedTicket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(resolvedTicket.getReopenCount()).isEqualTo(0);

        // When: Reopen from RESOLVED → NEW
        String reopenReason = "Issue reoccurred after initial fix, requires further investigation";
        Ticket reopenedTicket = ticketService.reopenTicket(ticketId, reopenReason, requesterId);
        log.info("Reopened ticket {} from RESOLVED, status: {}, reopenCount: {}",
                ticketId, reopenedTicket.getStatus(), reopenedTicket.getReopenCount());

        // Then: Verify NEW state and reopenCount incremented
        Ticket reloadedAfterReopen1 = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterReopen1.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(reloadedAfterReopen1.getReopenCount()).isEqualTo(1);
        assertThat(reloadedAfterReopen1.getAssigneeId()).isNull(); // Assignment cleared

        // For Incident, verify resolution fields cleared
        if (reloadedAfterReopen1 instanceof Incident incident) {
            assertThat(incident.getResolvedAt()).isNull();
            assertThat(incident.getResolutionNotes()).isNull();
        }

        // Given: Resolve and close the ticket again
        ticketService.assignTicket(ticketId, agentId, requesterId);
        ticketService.startWork(ticketId, agentId);
        ticketService.resolveTicket(ticketId, "Permanent fix applied with configuration update", agentId);
        ticketService.closeTicket(ticketId, requesterId);

        Ticket closedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(closedTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closedTicket.getReopenCount()).isEqualTo(1);

        // When: Reopen from CLOSED → NEW
        String reopenReason2 = "Customer reports issue still occurring, requires escalation";
        Ticket reopenedFromClosed = ticketService.reopenTicket(ticketId, reopenReason2, requesterId);
        log.info("Reopened ticket {} from CLOSED, status: {}, reopenCount: {}",
                ticketId, reopenedFromClosed.getStatus(), reopenedFromClosed.getReopenCount());

        // Then: Verify NEW state and reopenCount incremented again
        Ticket reloadedAfterReopen2 = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedAfterReopen2.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(reloadedAfterReopen2.getReopenCount()).isEqualTo(2);
        assertThat(reloadedAfterReopen2.getAssigneeId()).isNull();

        log.info("Test passed: Ticket reopened twice, reopenCount = {}", reloadedAfterReopen2.getReopenCount());
    }

    @Test
    @DisplayName("IT-SM-3: Invalid transitions - throw InvalidStateTransitionException (AC-20)")
    void invalidTransitions_shouldThrowInvalidStateTransitionException() {
        // Given: Create a ticket in NEW status
        CreateTicketCommand command = new CreateTicketCommand(
                "Test invalid transitions",
                "Testing state machine validation",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                "Testing",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        // Test 1: NEW → RESOLVED (must go through ASSIGNED → IN_PROGRESS)
        assertThatThrownBy(() -> ticketService.resolveTicket(ticketId, "Cannot resolve from NEW", agentId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid state transition");
        log.info("Test 1 passed: NEW → RESOLVED is invalid");

        // Given: Move ticket to CLOSED state
        ticketService.assignTicket(ticketId, agentId, requesterId); // NEW → ASSIGNED
        ticketService.startWork(ticketId, agentId); // ASSIGNED → IN_PROGRESS
        ticketService.resolveTicket(ticketId, "Resolved for testing purposes", agentId); // IN_PROGRESS → RESOLVED
        ticketService.closeTicket(ticketId, requesterId); // RESOLVED → CLOSED

        Ticket closedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(closedTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);

        // Test 2: CLOSED → ASSIGNED (must reopen first)
        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, agentId, requesterId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid state transition");
        log.info("Test 2 passed: CLOSED → ASSIGNED is invalid");

        // Given: Create another ticket and move to IN_PROGRESS
        CreateTicketCommand command2 = new CreateTicketCommand(
                "Test IN_PROGRESS transitions",
                "Testing IN_PROGRESS state validation",
                TicketType.INCIDENT,
                Priority.LOW,
                "Testing",
                requesterId
        );
        Ticket ticket2 = ticketService.createTicket(command2, requesterId);
        UUID ticketId2 = ticket2.getId();

        ticketService.assignTicket(ticketId2, agentId, requesterId);
        ticketService.startWork(ticketId2, agentId);

        Ticket inProgressTicket = ticketRepository.findById(ticketId2).orElseThrow();
        assertThat(inProgressTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        // Test 3: IN_PROGRESS → CLOSED (must resolve first)
        assertThatThrownBy(() -> ticketService.closeTicket(ticketId2, requesterId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Invalid state transition");
        log.info("Test 3 passed: IN_PROGRESS → CLOSED is invalid");

        log.info("Test passed: All invalid transitions correctly blocked");
    }

    @Test
    @DisplayName("IT-SM-4: Authorization checks - startWork/resolve by non-assignee throws UnauthorizedOperationException (AC-20)")
    void authorizationChecks_shouldThrowUnauthorizedOperationException_forNonAssignee() {
        // Given: Create and assign a ticket to agentId
        CreateTicketCommand command = new CreateTicketCommand(
                "Test authorization checks",
                "Verifying authorization enforcement",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                "Security",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        ticketService.assignTicket(ticketId, agentId, requesterId);

        Ticket assignedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(assignedTicket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(assignedTicket.getAssigneeId()).isEqualTo(agentId);

        // Test 1: startWork() by non-assignee (otherUserId)
        assertThatThrownBy(() -> ticketService.startWork(ticketId, otherUserId))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not authorized")
                .hasMessageContaining("startWork");
        log.info("Test 1 passed: startWork() by non-assignee blocked");

        // Given: Start work as correct assignee
        ticketService.startWork(ticketId, agentId);

        Ticket inProgressTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(inProgressTicket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

        // Test 2: resolve() by non-assignee (otherUserId)
        assertThatThrownBy(() -> ticketService.resolveTicket(ticketId, "Unauthorized resolution attempt", otherUserId))
                .isInstanceOf(UnauthorizedOperationException.class)
                .hasMessageContaining("not authorized")
                .hasMessageContaining("resolve");
        log.info("Test 2 passed: resolve() by non-assignee blocked");

        log.info("Test passed: Authorization checks enforce assignee-only operations");
    }

    @Test
    @DisplayName("IT-SM-5: Required field validation - assign/resolve/reopen throw MissingRequiredFieldException (AC-20)")
    void requiredFieldValidation_shouldThrowMissingRequiredFieldException() {
        // Given: Create a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test required field validation",
                "Verifying field validation enforcement",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                "Validation",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        // Test 1: assign() without assigneeId (null)
        assertThatThrownBy(() -> ticketService.assignTicket(ticketId, null, requesterId))
                .isInstanceOf(MissingRequiredFieldException.class)
                .hasMessageContaining("assigneeId")
                .hasMessageContaining("assign");
        log.info("Test 1 passed: assign() without assigneeId blocked");

        // Given: Assign ticket and start work
        ticketService.assignTicket(ticketId, agentId, requesterId);
        ticketService.startWork(ticketId, agentId);

        // Test 2: resolve() without resolutionNotes (null)
        assertThatThrownBy(() -> ticketService.resolveTicket(ticketId, null, agentId))
                .isInstanceOf(MissingRequiredFieldException.class)
                .hasMessageContaining("resolutionNotes")
                .hasMessageContaining("resolve");
        log.info("Test 2 passed: resolve() without resolutionNotes blocked");

        // Test 3: resolve() with resolutionNotes too short (< 10 characters)
        assertThatThrownBy(() -> ticketService.resolveTicket(ticketId, "Too short", agentId))
                .isInstanceOf(MissingRequiredFieldException.class)
                .hasMessageContaining("resolutionNotes")
                .hasMessageContaining("resolve");
        log.info("Test 3 passed: resolve() with short resolutionNotes blocked");

        // Given: Resolve ticket properly
        ticketService.resolveTicket(ticketId, "Proper resolution with sufficient detail and explanation", agentId);

        // Test 4: reopen() without reopenReason (null)
        assertThatThrownBy(() -> ticketService.reopenTicket(ticketId, null, requesterId))
                .isInstanceOf(MissingRequiredFieldException.class)
                .hasMessageContaining("reopenReason")
                .hasMessageContaining("reopen");
        log.info("Test 4 passed: reopen() without reopenReason blocked");

        // Test 5: reopen() with reopenReason too short (< 10 characters)
        assertThatThrownBy(() -> ticketService.reopenTicket(ticketId, "Short", requesterId))
                .isInstanceOf(MissingRequiredFieldException.class)
                .hasMessageContaining("reopenReason")
                .hasMessageContaining("reopen");
        log.info("Test 5 passed: reopen() with short reopenReason blocked");

        log.info("Test passed: All required field validations enforce minimum requirements");
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

    /**
     * Counts the number of events in the outbox for a specific ticket.
     */
    private int countEventsForTicket(UUID ticketId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE aggregate_id = ?",
                Integer.class,
                ticketId
        );
        return count != null ? count : 0;
    }
}
