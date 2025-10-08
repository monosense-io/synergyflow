package io.monosense.synergyflow.eventing.internal;

import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.pm.internal.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Integration test verifying that domain services publish events to the outbox.
 * Uses Testcontainers to test against a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EventPublishingIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketService ticketService;

    @Autowired
    private IssueService issueService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private io.monosense.synergyflow.eventing.api.EventPublisher eventPublisher;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private UUID testUserId;

    @BeforeEach
    void createTestUser() {
        // Create a test user to satisfy FK constraints
        testUserId = UUID.randomUUID();
        String uniqueUsername = "testuser-" + testUserId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                testUserId, uniqueUsername, uniqueEmail, "Test User", true, 1
        );

        // Ensure a clean outbox per test
        outboxRepository.deleteAll();
    }

    @Test
    void ticketCreatedEventIsPublished() {
        // Given
        UUID createdBy = testUserId;

        // When
        UUID ticketId = ticketService.createTicket("Test Ticket", "HIGH", createdBy);

        // Then
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(ticketId);
        assertThat(event.getAggregateType()).isEqualTo("TICKET");
        assertThat(event.getEventType()).isEqualTo("TicketCreated");
        assertThat(event.getVersion()).isEqualTo(1L); // First version after creation
        assertThat(event.getEventPayload()).isNotNull();
        assertThat(event.getEventPayload().get("aggregate_type").asText()).isEqualTo("TICKET");
        assertThat(event.getEventPayload().get("event_type").asText()).isEqualTo("TicketCreated");
        assertThat(event.getEventPayload().get("version").asLong()).isEqualTo(1L);
        assertThat(event.getEventPayload().get("payload").get("ticketId").asText()).isEqualTo(ticketId.toString());
    }

    @Test
    void ticketAssignedEventIsPublished() {
        // Given
        UUID createdBy = testUserId;
        UUID assigneeId = UUID.randomUUID();
        // Create assignee user to satisfy FK
        String assigneeUsername = "assignee-" + assigneeId.toString().substring(0, 8);
        String assigneeEmail = assigneeUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                assigneeId, assigneeUsername, assigneeEmail, "Assignee User", true, 1
        );
        UUID ticketId = ticketService.createTicket("Test Ticket", "HIGH", createdBy);
        outboxRepository.deleteAll(); // Clear the created event

        // When
        ticketService.assignTicket(ticketId, assigneeId, createdBy);

        // Then
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(ticketId);
        assertThat(event.getAggregateType()).isEqualTo("TICKET");
        assertThat(event.getEventType()).isEqualTo("TicketAssigned");
        assertThat(event.getVersion()).isEqualTo(2L); // Version incremented from 1 to 2
        assertThat(event.getEventPayload().get("payload").get("assigneeId").asText()).isEqualTo(assigneeId.toString());
    }

    @Test
    void issueCreatedEventIsPublished() {
        // Given
        UUID createdBy = testUserId;

        // When
        UUID issueId = issueService.createIssue("Test Issue", "BUG", createdBy);

        // Then
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(issueId);
        assertThat(event.getAggregateType()).isEqualTo("ISSUE");
        assertThat(event.getEventType()).isEqualTo("IssueCreated");
        assertThat(event.getVersion()).isEqualTo(1L);
        assertThat(event.getEventPayload().get("payload").get("issueId").asText()).isEqualTo(issueId.toString());
    }

    @Test
    void issueStateChangedEventIsPublished() {
        // Given
        UUID createdBy = testUserId;
        UUID issueId = issueService.createIssue("Test Issue", "BUG", createdBy);
        outboxRepository.deleteAll(); // Clear the created event

        // When
        issueService.changeIssueState(issueId, "IN_PROGRESS", createdBy);

        // Then
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(issueId);
        assertThat(event.getAggregateType()).isEqualTo("ISSUE");
        assertThat(event.getEventType()).isEqualTo("IssueStateChanged");
        assertThat(event.getVersion()).isEqualTo(2L); // Version incremented
        assertThat(event.getEventPayload().get("payload").get("fromState").asText()).isEqualTo("TODO");
        assertThat(event.getEventPayload().get("payload").get("toState").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void versionsIncrementSequentially() {
        // Given
        UUID createdBy = testUserId;
        UUID assigneeId = UUID.randomUUID();
        String assigneeUsername = "assignee-" + assigneeId.toString().substring(0, 8);
        String assigneeEmail = assigneeUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                assigneeId, assigneeUsername, assigneeEmail, "Assignee User", true, 1
        );

        // When - create ticket (version 1), assign twice (versions 2, 3)
        UUID ticketId = ticketService.createTicket("Test Ticket", "HIGH", createdBy);
        ticketService.assignTicket(ticketId, assigneeId, createdBy);
        ticketService.assignTicket(ticketId, assigneeId, createdBy);

        // Then - verify all 3 events with sequential versions
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(3);
        assertThat(events).extracting(OutboxEvent::getVersion)
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void duplicatePublishIsRejectedAndDoesNotCreateExtraRow() {
        // Given
        UUID createdBy = testUserId;
        UUID ticketId = ticketService.createTicket("Dup Ticket", "MEDIUM", createdBy);
        assertThat(outboxRepository.findAll()).hasSize(1);

        // When/Then - attempt duplicate publish for same aggregate/version
        assertThatThrownBy(() -> eventPublisher.publish(
                ticketId,
                "TICKET",
                "TicketCreated",
                1L,
                java.time.Instant.now(),
                objectMapper.createObjectNode()
        )).isInstanceOf(io.monosense.synergyflow.eventing.api.DuplicateEventException.class);

        // Outbox still has a single row for that aggregate/version
        assertThat(outboxRepository.findAll()).hasSize(1);
    }
}
