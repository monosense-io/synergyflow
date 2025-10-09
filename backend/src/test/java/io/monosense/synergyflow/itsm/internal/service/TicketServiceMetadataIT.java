package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.api.dto.AddCommentCommand;
import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketComment;
import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException;
import io.monosense.synergyflow.itsm.internal.repository.TicketCommentRepository;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for TicketService metadata operations (Task 7.6).
 *
 * <p>Tests verify comment creation, priority updates, and business rule enforcement
 * for metadata changes. Uses Testcontainers PostgreSQL for realistic database
 * persistence testing.</p>
 *
 * <p>Covers AC-23 (IT-META-1 to IT-META-3):
 * <ul>
 *   <li>IT-META-1: Add comment - verify comment persisted to ticket_comments table</li>
 *   <li>IT-META-2: Update priority - verify priority updated and event published</li>
 *   <li>IT-META-3: Update priority on resolved ticket - throws InvalidStateTransitionException</li>
 * </ul>
 *
 * @since 2.2
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TicketService Metadata Integration Tests (Task 7.6)")
class TicketServiceMetadataIT {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceMetadataIT.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository ticketCommentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID requesterId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        // Create test users
        requesterId = createUser("requester");
        agentId = createUser("agent");
    }

    @Test
    @DisplayName("IT-META-1: Add comment - verify comment persisted to ticket_comments table (AC-23)")
    void addComment_shouldPersistComment_toTicketCommentsTable() {
        // Given: Create a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Database performance issue",
                "Queries running slowly on production database",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Database",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        log.info("Created ticket {} for comment testing", ticketId);

        // When: Add a comment to the ticket
        AddCommentCommand commentCommand = new AddCommentCommand(
                "Initial investigation shows high CPU usage on database server. Checking query logs.",
                false // Public comment
        );
        TicketComment comment = ticketService.addComment(ticketId, commentCommand, agentId);

        log.info("Added comment {} to ticket {}", comment.getId(), ticketId);

        // Then: Verify comment persisted with correct data
        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getTicketId()).isEqualTo(ticketId);
        assertThat(comment.getAuthor()).isEqualTo(agentId);
        assertThat(comment.getCommentText()).isEqualTo(commentCommand.commentText());
        assertThat(comment.isInternal()).isEqualTo(commentCommand.isInternal());
        assertThat(comment.getCreatedAt()).isNotNull();

        // Verify comment exists in database via repository
        TicketComment persistedComment = ticketCommentRepository.findById(comment.getId()).orElseThrow();
        assertThat(persistedComment.getTicketId()).isEqualTo(ticketId);
        assertThat(persistedComment.getCommentText()).isEqualTo(commentCommand.commentText());

        // Verify comment can be queried by ticket ID
        List<TicketComment> ticketComments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        assertThat(ticketComments).hasSize(1);
        assertThat(ticketComments.get(0).getId()).isEqualTo(comment.getId());

        // When: Add an internal comment
        AddCommentCommand internalCommentCommand = new AddCommentCommand(
                "Internal note: Need to escalate to database team if not resolved within 2 hours.",
                true // Internal comment
        );
        TicketComment internalComment = ticketService.addComment(ticketId, internalCommentCommand, agentId);

        log.info("Added internal comment {} to ticket {}", internalComment.getId(), ticketId);

        // Then: Verify both comments persisted
        List<TicketComment> allComments = ticketCommentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        assertThat(allComments).hasSize(2);
        assertThat(allComments.get(0).isInternal()).isFalse(); // First comment (public)
        assertThat(allComments.get(1).isInternal()).isTrue();  // Second comment (internal)

        log.info("Test passed: {} comments persisted to ticket_comments table", allComments.size());
    }

    @Test
    @DisplayName("IT-META-2: Update priority - verify priority updated and event published (AC-23)")
    void updatePriority_shouldUpdatePriorityAndPublishEvent() {
        // Given: Create a ticket with MEDIUM priority
        CreateTicketCommand command = new CreateTicketCommand(
                "Network latency issue",
                "Users reporting slow network response times",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                "Network",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        log.info("Created ticket {} with MEDIUM priority", ticketId);

        // Verify initial priority
        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);

        // Count initial events
        int initialEventCount = countEventsForTicket(ticketId);

        // When: Escalate priority to HIGH
        Ticket updatedTicket = ticketService.updatePriority(ticketId, Priority.HIGH, agentId);

        log.info("Updated ticket {} priority to HIGH", ticketId);

        // Then: Verify priority updated in returned ticket
        assertThat(updatedTicket.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(updatedTicket.getVersion()).isGreaterThan(ticket.getVersion());

        // Verify priority persisted in database
        Ticket reloadedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(reloadedTicket.getPriority()).isEqualTo(Priority.HIGH);

        // Verify event published (TicketStateChanged event for priority update)
        int finalEventCount = countEventsForTicket(ticketId);
        assertThat(finalEventCount).isGreaterThan(initialEventCount);

        log.info("Test passed: Priority updated from MEDIUM to HIGH, event published");

        // When: Further escalate to CRITICAL
        Ticket criticalTicket = ticketService.updatePriority(ticketId, Priority.CRITICAL, agentId);

        // Then: Verify second update
        assertThat(criticalTicket.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(criticalTicket.getVersion()).isGreaterThan(updatedTicket.getVersion());

        Ticket finalTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(finalTicket.getPriority()).isEqualTo(Priority.CRITICAL);

        log.info("Test passed: Priority updated to CRITICAL");
    }

    @Test
    @DisplayName("IT-META-3: Update priority on resolved ticket - throws InvalidStateTransitionException (AC-23)")
    void updatePriority_shouldThrowInvalidStateTransitionException_whenTicketResolved() {
        // Given: Create a ticket and resolve it
        CreateTicketCommand command = new CreateTicketCommand(
                "Email server timeout",
                "Email delivery delayed due to server timeout",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Email",
                requesterId
        );
        Ticket ticket = ticketService.createTicket(command, requesterId);
        UUID ticketId = ticket.getId();

        // Progress ticket through lifecycle to RESOLVED
        ticketService.assignTicket(ticketId, agentId, requesterId);
        ticketService.startWork(ticketId, agentId);
        ticketService.resolveTicket(ticketId, "Email server restarted and timeout issue resolved", agentId);

        // Verify ticket is in RESOLVED state
        Ticket resolvedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(resolvedTicket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(resolvedTicket.getPriority()).isEqualTo(Priority.HIGH);

        log.info("Ticket {} is in RESOLVED state with priority HIGH", ticketId);

        // When/Then: Attempt to update priority on resolved ticket
        assertThatThrownBy(() -> ticketService.updatePriority(ticketId, Priority.CRITICAL, agentId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Priority cannot be changed");

        log.info("Test passed: Priority update blocked on RESOLVED ticket");

        // Given: Close the ticket
        ticketService.closeTicket(ticketId, requesterId);

        Ticket closedTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(closedTicket.getStatus()).isEqualTo(TicketStatus.CLOSED);

        // When/Then: Attempt to update priority on closed ticket
        assertThatThrownBy(() -> ticketService.updatePriority(ticketId, Priority.CRITICAL, agentId))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Priority cannot be changed");

        log.info("Test passed: Priority update blocked on CLOSED ticket");

        // Verify priority remained unchanged
        Ticket finalTicket = ticketRepository.findById(ticketId).orElseThrow();
        assertThat(finalTicket.getPriority()).isEqualTo(Priority.HIGH);
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
