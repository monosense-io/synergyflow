package io.monosense.synergyflow.itsm.internal.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.*;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for audit event publishing in TicketService (Task 12).
 *
 * <p>Tests verify that sensitive operations (assign, unassign, reassign, resolve, reopen)
 * publish audit events to the transactional outbox for compliance tracking and security auditing.</p>
 *
 * <p>Covers Task 12.3: Write integration tests to verify audit events published to outbox
 * <ul>
 *   <li>IT-AUDIT-1: Assignment operations publish TicketAssignmentAudit</li>
 *   <li>IT-AUDIT-2: Resolution operations publish TicketResolutionAudit</li>
 *   <li>IT-AUDIT-3: Reopen operations publish TicketReopenAudit</li>
 *   <li>IT-AUDIT-4: Audit events include complete operation context</li>
 * </ul>
 *
 * @since 2.2
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("TicketService Audit Event Integration Tests (Task 12)")
class TicketServiceAuditIT {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceAuditIT.class);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private UUID agent1Id;
    private UUID agent2Id;

    @BeforeEach
    void setUp() {
        // Create test users
        testUserId = createUser("requester");
        agent1Id = createUser("agent1");
        agent2Id = createUser("agent2");
    }

    @Test
    @DisplayName("IT-AUDIT-1: Assign operation publishes TicketAssignmentAudit with operation=ASSIGN")
    void assignTicket_shouldPublishAssignmentAuditEvent() {
        // Given: Create a ticket in NEW status
        CreateTicketCommand command = new CreateTicketCommand(
                "Test assignment audit",
                "Testing audit event for assignment operation",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();

        // Clear events from creation
        int initialEventCount = countEventsForTicket(ticketId);
        log.info("Initial event count after ticket creation: {}", initialEventCount);

        // When: Assign the ticket
        ticketService.assignTicket(ticketId, agent1Id, testUserId);

        // Then: Verify TicketAssignmentAudit event was published
        // Debug: Check all events for this ticket
        List<Map<String, Object>> allEvents = jdbcTemplate.queryForList(
                "SELECT event_type, aggregate_type FROM outbox WHERE aggregate_id = ? ORDER BY occurred_at",
                ticketId
        );
        log.info("All events for ticket {}: {}", ticketId, allEvents);

        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketAssignmentAudit");
        log.info("Found {} TicketAssignmentAudit events", auditEvents.size());
        assertThat(auditEvents).hasSize(1);

        Map<String, Object> auditEvent = auditEvents.get(0);
        assertThat(auditEvent.get("event_type")).isEqualTo("TicketAssignmentAudit");

        // Verify audit event payload
        JsonNode payload = parsePayload(auditEvent.get("event_payload"));
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("currentAssigneeId").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("previousAssigneeId").isNull()).isTrue();
        assertThat(payload.get("performedBy").asText()).isEqualTo(testUserId.toString());
        assertThat(payload.get("operation").asText()).isEqualTo("ASSIGN");

        log.info("Test passed: TicketAssignmentAudit event published for ASSIGN operation");
    }

    @Test
    @DisplayName("IT-AUDIT-2: Unassign operation publishes TicketAssignmentAudit with operation=UNASSIGN")
    void unassignTicket_shouldPublishAssignmentAuditEvent() {
        // Given: Create and assign a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test unassignment audit",
                "Testing audit event for unassignment operation",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();
        ticketService.assignTicket(ticketId, agent1Id, testUserId);

        // When: Unassign the ticket
        ticketService.unassignTicket(ticketId, agent1Id);

        // Then: Verify TicketAssignmentAudit event for UNASSIGN was published
        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketAssignmentAudit");
        assertThat(auditEvents).hasSizeGreaterThanOrEqualTo(2); // One for ASSIGN, one for UNASSIGN

        // Find the UNASSIGN audit event
        Map<String, Object> unassignEvent = auditEvents.stream()
                .filter(event -> {
                    JsonNode payload = parsePayload(event.get("event_payload"));
                    return "UNASSIGN".equals(payload.get("operation").asText());
                })
                .findFirst()
                .orElseThrow();

        JsonNode payload = parsePayload(unassignEvent.get("event_payload"));
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("currentAssigneeId").isNull()).isTrue();
        assertThat(payload.get("previousAssigneeId").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("performedBy").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("operation").asText()).isEqualTo("UNASSIGN");

        log.info("Test passed: TicketAssignmentAudit event published for UNASSIGN operation");
    }

    @Test
    @DisplayName("IT-AUDIT-3: Reassign operation publishes TicketAssignmentAudit with operation=REASSIGN")
    void reassignTicket_shouldPublishAssignmentAuditEvent() {
        // Given: Create and assign a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test reassignment audit",
                "Testing audit event for reassignment operation",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();
        ticketService.assignTicket(ticketId, agent1Id, testUserId);

        // When: Reassign the ticket to another agent
        ticketService.reassignTicket(ticketId, agent2Id, agent1Id);

        // Then: Verify TicketAssignmentAudit event for REASSIGN was published
        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketAssignmentAudit");
        assertThat(auditEvents).hasSizeGreaterThanOrEqualTo(2); // One for ASSIGN, one for REASSIGN

        // Find the REASSIGN audit event
        Map<String, Object> reassignEvent = auditEvents.stream()
                .filter(event -> {
                    JsonNode payload = parsePayload(event.get("event_payload"));
                    return "REASSIGN".equals(payload.get("operation").asText());
                })
                .findFirst()
                .orElseThrow();

        JsonNode payload = parsePayload(reassignEvent.get("event_payload"));
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("currentAssigneeId").asText()).isEqualTo(agent2Id.toString());
        assertThat(payload.get("previousAssigneeId").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("performedBy").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("operation").asText()).isEqualTo("REASSIGN");

        log.info("Test passed: TicketAssignmentAudit event published for REASSIGN operation");
    }

    @Test
    @DisplayName("IT-AUDIT-4: Resolve operation publishes TicketResolutionAudit")
    void resolveTicket_shouldPublishResolutionAuditEvent() {
        // Given: Create, assign, and start work on a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test resolution audit",
                "Testing audit event for resolution operation",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();
        ticketService.assignTicket(ticketId, agent1Id, testUserId);
        ticketService.startWork(ticketId, agent1Id);

        // When: Resolve the ticket
        String resolutionNotes = "Issue resolved by restarting the service and applying configuration fix";
        ticketService.resolveTicket(ticketId, resolutionNotes, agent1Id);

        // Then: Verify TicketResolutionAudit event was published
        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketResolutionAudit");
        assertThat(auditEvents).hasSize(1);

        Map<String, Object> auditEvent = auditEvents.get(0);
        assertThat(auditEvent.get("event_type")).isEqualTo("TicketResolutionAudit");

        JsonNode payload = parsePayload(auditEvent.get("event_payload"));
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("resolvedBy").asText()).isEqualTo(agent1Id.toString());
        assertThat(payload.get("resolutionNotes").asText()).isEqualTo(resolutionNotes);
        assertThat(payload.has("resolvedAt")).isTrue();
        assertThat(payload.has("version")).isTrue();

        log.info("Test passed: TicketResolutionAudit event published for resolve operation");
    }

    @Test
    @DisplayName("IT-AUDIT-5: Reopen operation publishes TicketReopenAudit with reopen count")
    void reopenTicket_shouldPublishReopenAuditEvent() {
        // Given: Create, assign, resolve, and close a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test reopen audit",
                "Testing audit event for reopen operation",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();
        ticketService.assignTicket(ticketId, agent1Id, testUserId);
        ticketService.startWork(ticketId, agent1Id);
        ticketService.resolveTicket(ticketId, "Fixed the issue initially", agent1Id);

        // When: Reopen the ticket
        String reopenReason = "Issue recurred, needs additional investigation and fix";
        ticketService.reopenTicket(ticketId, reopenReason, testUserId);

        // Then: Verify TicketReopenAudit event was published
        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketReopenAudit");
        assertThat(auditEvents).hasSize(1);

        Map<String, Object> auditEvent = auditEvents.get(0);
        assertThat(auditEvent.get("event_type")).isEqualTo("TicketReopenAudit");

        JsonNode payload = parsePayload(auditEvent.get("event_payload"));
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("reopenedBy").asText()).isEqualTo(testUserId.toString());
        assertThat(payload.get("reopenReason").asText()).isEqualTo(reopenReason);
        assertThat(payload.get("reopenCount").asInt()).isEqualTo(1);
        assertThat(payload.has("reopenedAt")).isTrue();
        assertThat(payload.has("version")).isTrue();

        log.info("Test passed: TicketReopenAudit event published for reopen operation");
    }

    @Test
    @DisplayName("IT-AUDIT-6: Multiple reopens increment reopen count in audit events")
    void reopenTicket_multipleReopens_shouldTrackReopenCount() {
        // Given: Create and resolve a ticket
        CreateTicketCommand command = new CreateTicketCommand(
                "Test multiple reopen audit",
                "Testing reopen count tracking in audit events",
                TicketType.INCIDENT,
                Priority.HIGH,
                "Infrastructure",
                testUserId
        );
        Ticket ticket = ticketService.createTicket(command, testUserId);
        UUID ticketId = ticket.getId();
        ticketService.assignTicket(ticketId, agent1Id, testUserId);
        ticketService.startWork(ticketId, agent1Id);
        ticketService.resolveTicket(ticketId, "Fixed the issue", agent1Id);

        // When: Reopen, resolve, and reopen again
        ticketService.reopenTicket(ticketId, "Issue recurred first time", testUserId);
        ticketService.assignTicket(ticketId, agent1Id, testUserId);
        ticketService.startWork(ticketId, agent1Id);
        ticketService.resolveTicket(ticketId, "Fixed the issue again", agent1Id);
        ticketService.reopenTicket(ticketId, "Issue recurred second time", testUserId);

        // Then: Verify both reopen audit events with incrementing counts
        List<Map<String, Object>> auditEvents = getAuditEventsByType(ticketId, "TicketReopenAudit");
        assertThat(auditEvents).hasSize(2);

        // First reopen should have count = 1
        JsonNode firstPayload = parsePayload(auditEvents.get(0).get("event_payload"));
        assertThat(firstPayload.get("reopenCount").asInt()).isEqualTo(1);

        // Second reopen should have count = 2
        JsonNode secondPayload = parsePayload(auditEvents.get(1).get("event_payload"));
        assertThat(secondPayload.get("reopenCount").asInt()).isEqualTo(2);

        log.info("Test passed: Multiple reopens correctly track reopen count in audit events");
    }

    // ===================================
    // Helper Methods
    // ===================================

    /**
     * Creates a test user in the database and returns the UUID.
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

    /**
     * Retrieves audit events of a specific type for a ticket from the outbox.
     */
    private List<Map<String, Object>> getAuditEventsByType(UUID ticketId, String eventType) {
        return jdbcTemplate.queryForList(
                "SELECT event_type, event_payload FROM outbox WHERE aggregate_id = ? AND event_type = ? ORDER BY occurred_at",
                ticketId,
                eventType
        );
    }

    /**
     * Parses JSON payload to JsonNode.
     * Handles both String and PGobject (PostgreSQL JSONB) types.
     */
    private JsonNode parsePayload(Object payload) {
        try {
            if (payload instanceof String) {
                return objectMapper.readTree((String) payload);
            } else if (payload instanceof org.postgresql.util.PGobject) {
                return objectMapper.readTree(((org.postgresql.util.PGobject) payload).getValue());
            } else if (payload instanceof com.fasterxml.jackson.databind.JsonNode) {
                return (com.fasterxml.jackson.databind.JsonNode) payload;
            } else {
                throw new IllegalArgumentException("Unexpected payload type: " + payload.getClass());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse event payload JSON", e);
        }
    }
}
