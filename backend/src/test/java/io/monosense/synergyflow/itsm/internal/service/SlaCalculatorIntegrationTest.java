package io.monosense.synergyflow.itsm.internal.service;
import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.SlaTrackingRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for SLA tracking functionality (Story 2.3 AC-9).
 *
 * <p>Tests verify SLA tracking behavior with real database (Testcontainers PostgreSQL):
 * <ul>
 *   <li>IT-SLA-1: SLA created on incident creation</li>
 *   <li>IT-SLA-2: SLA skipped for service request</li>
 *   <li>IT-SLA-3: SLA skipped for incident without priority</li>
 *   <li>IT-SLA-4: SLA recalculated on priority change</li>
 *   <li>IT-SLA-5: SLA frozen for resolved ticket</li>
 *   <li>IT-SLA-6: SLA created when priority added later</li>
 * </ul>
 *
 * @author monosense
 * @since 2.3
 */
@SpringBootTest
@Testcontainers
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
@DisplayName("SLA Calculator Integration Tests (Story 2.3)")
class SlaCalculatorIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SlaTrackingRepository slaTrackingRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID testUserId;
    private UUID testAgentId;

    @BeforeAll
    void setupTestUsers() {
        // Create test users to satisfy foreign key constraints
        testUserId = UUID.randomUUID();
        testAgentId = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, NOW(), NOW(), 1)",
            testUserId, "test-requester", "requester@test.com", "Test Requester"
        );
        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, NOW(), NOW(), 1)",
            testAgentId, "test-agent", "agent@test.com", "Test Agent"
        );
    }

    @AfterEach
    void cleanup() {
        // Clean up test data to prevent constraint violations between tests
        slaTrackingRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    /**
     * IT-SLA-1: SLA created on incident creation with CRITICAL priority (2h)
     */
    @Test
    @DisplayName("IT-SLA-1: SLA created on incident creation with priority")
    void createIncidentWithPriority_shouldCreateSlaRecord() {
        // Given
        CreateTicketCommand command = new CreateTicketCommand(
                "Critical Production Outage",
                "Database server is down",
                TicketType.INCIDENT,
                Priority.CRITICAL,
                "Infrastructure",
                testUserId
        );

        // When
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Then
        Optional<SlaTracking> slaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(slaOpt).isPresent();

        SlaTracking sla = slaOpt.get();
        assertThat(sla.getTicketId()).isEqualTo(ticket.getId());
        assertThat(sla.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(sla.isBreached()).isFalse();

        // Verify due_at = created_at + 2 hours (CRITICAL priority)
        Instant expectedDueAt = ticket.getCreatedAt().plus(Duration.ofHours(2));
        assertThat(sla.getDueAt()).isEqualTo(expectedDueAt);
    }

    /**
     * IT-SLA-2: SLA skipped for service request
     */
    @Test
    @DisplayName("IT-SLA-2: SLA skipped for service request")
    void createServiceRequest_shouldNotCreateSlaRecord() {
        // Given
        CreateTicketCommand command = new CreateTicketCommand(
                "Request New Laptop",
                "Need MacBook Pro for new developer",
                TicketType.SERVICE_REQUEST,
                Priority.MEDIUM,
                "Hardware",
                testUserId
        );

        // When
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Then
        Optional<SlaTracking> slaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(slaOpt).isEmpty();
    }

    /**
     * IT-SLA-3: SLA skipped for incident without priority
     */
    @Test
    @DisplayName("IT-SLA-3: SLA skipped for incident without priority")
    void createIncidentWithoutPriority_shouldNotCreateSlaRecord() {
        // Given: Create command with null priority
        CreateTicketCommand command = new CreateTicketCommand(
                "Printer Issue",
                "Printer not responding",
                TicketType.INCIDENT,
                null,  // No priority
                "Office Equipment",
                testUserId
        );

        // When
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Then
        Optional<SlaTracking> slaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(slaOpt).isEmpty();
    }

    /**
     * IT-SLA-4: SLA recalculated on priority change from LOW (24h) to CRITICAL (2h)
     *
     * <p>Verifies that due_at is recalculated from original ticket.createdAt, not current time.</p>
     */
    @Test
    @DisplayName("IT-SLA-4: SLA recalculated on priority change from original createdAt")
    void updatePriority_shouldRecalculateSlaFromOriginalCreatedAt() {
        // Given: Create LOW priority incident
        CreateTicketCommand command = new CreateTicketCommand(
                "Minor Performance Issue",
                "Application slow for some users",
                TicketType.INCIDENT,
                Priority.LOW,
                "Performance",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Verify initial SLA: created_at + 24h
        Optional<SlaTracking> initialSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(initialSlaOpt).isPresent();
        SlaTracking initialSla = initialSlaOpt.get();
        Instant initialDueAt = initialSla.getDueAt();
        assertThat(initialDueAt).isEqualTo(ticket.getCreatedAt().plus(Duration.ofHours(24)));

        // When: Escalate to CRITICAL
        ticketService.updatePriority(ticket.getId(), Priority.CRITICAL, testAgentId);

        // Then: SLA recalculated from ORIGINAL ticket.createdAt + 2h (not current time)
        Optional<SlaTracking> updatedSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(updatedSlaOpt).isPresent();
        SlaTracking updatedSla = updatedSlaOpt.get();
        Instant expectedNewDueAt = ticket.getCreatedAt().plus(Duration.ofHours(2));
        assertThat(updatedSla.getDueAt()).isEqualTo(expectedNewDueAt);
        assertThat(updatedSla.getPriority()).isEqualTo(Priority.CRITICAL);

        // New due_at should be earlier than original due_at (escalation scenario)
        assertThat(updatedSla.getDueAt()).isBefore(initialDueAt);
    }

    /**
     * IT-SLA-5: SLA frozen for resolved ticket (priority changes rejected by Story 2.2 validation)
     */
    @Test
    @DisplayName("IT-SLA-5: SLA frozen for resolved ticket")
    void updatePriorityOnResolvedTicket_shouldNotRecalculateSla() {
        // Given: Create and resolve a HIGH priority incident
        CreateTicketCommand command = new CreateTicketCommand(
                "Network Connectivity Issue",
                "Users cannot access internal network",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Network",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Capture original SLA before resolution
        Optional<SlaTracking> originalSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(originalSlaOpt).isPresent();
        SlaTracking originalSla = originalSlaOpt.get();
        Instant originalDueAt = originalSla.getDueAt();

        // Assign the ticket first (NEW → ASSIGNED)
        ticketService.assignTicket(ticket.getId(), testAgentId, testAgentId);

        // Start work on the ticket (ASSIGNED → IN_PROGRESS)
        ticketService.startWork(ticket.getId(), testAgentId);

        // Resolve the ticket (IN_PROGRESS → RESOLVED)
        ticketService.resolveTicket(ticket.getId(), "Network cable replaced", testAgentId);

        // When/Then: Attempt to change priority on resolved ticket should fail
        // (Story 2.2 validation prevents priority changes on RESOLVED tickets)
        assertThatThrownBy(() -> ticketService.updatePriority(ticket.getId(), Priority.CRITICAL, testAgentId))
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class)
                .hasMessageContaining("Priority cannot be changed on RESOLVED tickets");

        // Verify SLA unchanged (frozen at resolution time)
        Optional<SlaTracking> frozenSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(frozenSlaOpt).isPresent();
        SlaTracking frozenSla = frozenSlaOpt.get();
        assertThat(frozenSla.getDueAt()).isEqualTo(originalDueAt);
        assertThat(frozenSla.getPriority()).isEqualTo(Priority.HIGH); // Priority unchanged in SLA record
    }

    /**
     * IT-SLA-6: SLA created when priority added later to incident without initial priority
     */
    @Test
    @DisplayName("IT-SLA-6: SLA created when priority added later")
    void addPriorityToIncidentWithoutInitialPriority_shouldCreateSla() {
        // Given: Create incident without priority
        CreateTicketCommand command = new CreateTicketCommand(
                "Unassessed Issue",
                "Issue reported, priority to be determined",
                TicketType.INCIDENT,
                null,  // No initial priority
                "Triage",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testAgentId);

        // Verify no SLA record exists initially
        Optional<SlaTracking> initialSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(initialSlaOpt).isEmpty();

        // When: Add priority later
        ticketService.updatePriority(ticket.getId(), Priority.MEDIUM, testAgentId);

        // Then: SLA record created with due_at calculated from original ticket.createdAt
        Optional<SlaTracking> newSlaOpt = slaTrackingRepository.findByTicketId(ticket.getId());
        assertThat(newSlaOpt).isPresent();
        SlaTracking newSla = newSlaOpt.get();
        assertThat(newSla.getPriority()).isEqualTo(Priority.MEDIUM);

        // Verify due_at = original ticket.createdAt + 8h (MEDIUM priority)
        Instant expectedDueAt = ticket.getCreatedAt().plus(Duration.ofHours(8));
        assertThat(newSla.getDueAt()).isEqualTo(expectedDueAt);
    }
}
