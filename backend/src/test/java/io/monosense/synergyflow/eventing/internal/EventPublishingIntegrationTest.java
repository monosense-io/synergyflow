package io.monosense.synergyflow.eventing.internal;

import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.TicketService;
import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
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
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Ticket",
                "Test Description",
                TicketType.INCIDENT,
                Priority.HIGH,
                null,
                createdBy
        );

        // When
        UUID ticketId = ticketService.createTicket(command, createdBy).getId();

        // Then
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);

        OutboxEvent event = events.get(0);
        assertThat(event.getAggregateId()).isEqualTo(ticketId);
        assertThat(event.getAggregateType()).isEqualTo("TICKET");
        assertThat(event.getEventType()).isEqualTo("TicketCreated");
        assertThat(event.getVersion()).isEqualTo(1L); // First version after creation
        assertThat(event.getEventPayload()).isNotNull();
        // Envelope fields are available on the entity, payload contains raw domain event
        assertThat(event.getAggregateType()).isEqualTo("TICKET");
        assertThat(event.getEventType()).isEqualTo("TicketCreated");
        assertThat(event.getVersion()).isEqualTo(1L);
        assertThat(event.getEventPayload().get("ticketId").asText()).isEqualTo(ticketId.toString());
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
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Ticket",
                "Test Description",
                TicketType.INCIDENT,
                Priority.HIGH,
                null,
                createdBy
        );
        UUID ticketId = ticketService.createTicket(command, createdBy).getId();
        outboxRepository.deleteAll(); // Clear the created event

        // When
        ticketService.assignTicket(ticketId, assigneeId, createdBy);

        // Then: two events are published (business + audit). Assert the business event.
        List<OutboxEvent> assignedEvents = outboxRepository.findAll().stream()
                .filter(e -> e.getEventType().equals("TicketAssigned"))
                .toList();
        assertThat(assignedEvents).hasSize(1);

        OutboxEvent event = assignedEvents.get(0);
        assertThat(event.getAggregateId()).isEqualTo(ticketId);
        assertThat(event.getAggregateType()).isEqualTo("TICKET");
        assertThat(event.getEventType()).isEqualTo("TicketAssigned");
        assertThat(event.getVersion()).isEqualTo(2L); // Version incremented from 1 to 2
        assertThat(event.getEventPayload().get("assigneeId").asText()).isEqualTo(assigneeId.toString());
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
        assertThat(event.getEventPayload().get("issueId").asText()).isEqualTo(issueId.toString());
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
        assertThat(event.getEventPayload().get("fromState").asText()).isEqualTo("TODO");
        assertThat(event.getEventPayload().get("toState").asText()).isEqualTo("IN_PROGRESS");
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

        // When - create ticket (version 1), assign once (version 2), start work (version 3)
        CreateTicketCommand command = new CreateTicketCommand(
                "Test Ticket",
                "Test Description",
                TicketType.INCIDENT,
                Priority.HIGH,
                null,
                createdBy
        );
        UUID ticketId = ticketService.createTicket(command, createdBy).getId();
        ticketService.assignTicket(ticketId, assigneeId, createdBy);
        ticketService.startWork(ticketId, assigneeId);

        // Then - verify versions reach 3 sequentially; assignment emits an extra audit event at version 2
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(4);
        assertThat(events.stream().map(OutboxEvent::getVersion).distinct().toList())
                .containsExactly(1L, 2L, 3L);
    }

    @Test
    void duplicatePublishIsRejectedAndDoesNotCreateExtraRow() {
        // Given
        UUID createdBy = testUserId;
        CreateTicketCommand command = new CreateTicketCommand(
                "Dup Ticket",
                "Test Description",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                null,
                createdBy
        );
        UUID ticketId = ticketService.createTicket(command, createdBy).getId();
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
