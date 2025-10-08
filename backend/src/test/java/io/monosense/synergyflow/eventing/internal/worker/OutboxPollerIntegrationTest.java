package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.SynergyFlowApplication;
import io.monosense.synergyflow.eventing.worker.model.TicketAssignedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@SpringBootTest(classes = SynergyFlowApplication.class,
        properties = {
                "synergyflow.outbox.polling.fixed-delay-ms=50",
                "synergyflow.outbox.polling.batch-size=10",
                "spring.task.scheduling.enabled=false"
        })
@ActiveProfiles("worker")
@Testcontainers
class OutboxPollerIntegrationTest {

    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
    private static final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> REDIS_HOST);
        registry.add("spring.redis.port", () -> REDIS_PORT);
        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
            registry.add("spring.redis.password", () -> REDIS_PASSWORD);
        }
        registry.add("WORKER_MANAGEMENT_PORT", () -> "0");
        registry.add("WORKER_ACTUATOR_PASSWORD", () -> "integration-actuator");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxPoller outboxPoller;

    @Autowired
    private GapDetector gapDetector;

    @Autowired
    private MeterRegistry meterRegistry;

    @MockitoBean
    private RedisStreamPublisher redisStreamPublisher;

    @MockitoBean
    private org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;

    @Test
    void processesTicketEventsAndUpdatesReadModel() throws Exception {
        UUID ticketId = UUID.randomUUID();
        Instant now = Instant.parse("2025-10-07T10:00:00Z");
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID assignedById = UUID.randomUUID();

        insertUser(requesterId, "requester");
        insertUser(assigneeId, "assignee");
        insertUser(assignedById, "assigner");
        insertTicket(ticketId, requesterId, "T-" + ticketId.toString().substring(0, 8), now);

        insertTicketCreatedEvent(ticketId, now, requesterId);

        doNothing().when(redisStreamPublisher).publish(any(OutboxMessage.class));

        outboxPoller.poll();

        Integer ticketRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ticket_card WHERE ticket_id = ?",
                Integer.class,
                ticketId);
        assertThat(ticketRows).isEqualTo(1);

        Long readModelVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM ticket_card WHERE ticket_id = ?",
                Long.class,
                ticketId);
        assertThat(readModelVersion).isEqualTo(1L);

        insertTicketAssignedEvent(ticketId, now.plusSeconds(5), assigneeId, assignedById);

        outboxPoller.poll();

        String assigneeName = jdbcTemplate.queryForObject(
                "SELECT assignee_name FROM ticket_card WHERE ticket_id = ?",
                String.class,
                ticketId);
        assertThat(assigneeName).isEqualTo("Agent Smith");

        Long updatedVersion = jdbcTemplate.queryForObject(
                "SELECT version FROM ticket_card WHERE ticket_id = ?",
                Long.class,
                ticketId);
        assertThat(updatedVersion).isEqualTo(2L);

        Integer processedRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM outbox WHERE processed_at IS NOT NULL AND aggregate_id = ?",
                Integer.class,
                ticketId);
        assertThat(processedRows).isEqualTo(2);
    }

    @Test
    void gapDetectorRaisesMetricForUnprocessedRowsWithinRange() throws Exception {
        Instant baseTime = Instant.parse("2025-10-07T12:00:00Z");

        UUID processedOne = UUID.randomUUID();
        UUID gapAggregate = UUID.randomUUID();
        UUID processedTwo = UUID.randomUUID();

        persistOutboxRow(processedOne, "TicketCreated", 1L, baseTime, buildCreatedPayload(processedOne, baseTime));
        persistOutboxRow(gapAggregate, "TicketCreated", 1L, baseTime.plusSeconds(1), buildCreatedPayload(gapAggregate, baseTime.plusSeconds(1)));
        persistOutboxRow(processedTwo, "TicketCreated", 1L, baseTime.plusSeconds(2), buildCreatedPayload(processedTwo, baseTime.plusSeconds(2)));

        Instant processedAt = baseTime.plusSeconds(10);
        jdbcTemplate.update("UPDATE outbox SET processed_at = ? WHERE aggregate_id = ?",
                Timestamp.from(processedAt), processedOne);
        jdbcTemplate.update("UPDATE outbox SET processed_at = ? WHERE aggregate_id = ?",
                Timestamp.from(processedAt), processedTwo);

        double before = meterRegistry.find("outbox_gaps_detected_total").counter() != null
                ? meterRegistry.find("outbox_gaps_detected_total").counter().count()
                : 0.0;

        gapDetector.detectGaps();

        double after = meterRegistry.get("outbox_gaps_detected_total").counter().count();
        assertThat(after - before).isEqualTo(1.0);
    }

    private void insertTicketCreatedEvent(UUID ticketId, Instant occurredAt, UUID requesterId) throws SQLException, JsonProcessingException {
        TicketCreatedMessage message = new TicketCreatedMessage(
                ticketId,
                "T-" + ticketId.toString().substring(0, 8),
                "New ticket created",
                "OPEN",
                "HIGH",
                requesterId,
                "Requester Name",
                "requester@example.com",
                occurredAt,
                occurredAt
        );
        JsonNode payload = objectMapper.valueToTree(message);
        persistOutboxRow(ticketId, "TicketCreated", 1L, occurredAt, payload);
    }

    private JsonNode buildCreatedPayload(UUID ticketId, Instant occurredAt) {
        TicketCreatedMessage message = new TicketCreatedMessage(
                ticketId,
                "T-" + ticketId.toString().substring(0, 8),
                "Generated ticket",
                "OPEN",
                "MEDIUM",
                UUID.randomUUID(),
                "Requester",
                "requester@example.com",
                occurredAt,
                occurredAt
        );
        return objectMapper.valueToTree(message);
    }

    private void insertTicketAssignedEvent(UUID ticketId, Instant occurredAt, UUID assigneeId, UUID assignedById) throws SQLException, JsonProcessingException {
        TicketAssignedMessage message = new TicketAssignedMessage(
                ticketId,
                assigneeId,
                assignedById,
                "Agent Smith",
                "agent.smith@example.com",
                occurredAt,
                2L
        );
        JsonNode payload = objectMapper.valueToTree(message);
        persistOutboxRow(ticketId, "TicketAssigned", 2L, occurredAt, payload);
    }

    private void persistOutboxRow(UUID aggregateId,
                                  String eventType,
                                  long version,
                                  Instant occurredAt,
                                  JsonNode payload) throws SQLException, JsonProcessingException {
        PGobject jsonb = new PGobject();
        jsonb.setType("jsonb");
        jsonb.setValue(objectMapper.writeValueAsString(payload));

        jdbcTemplate.update(
                "INSERT INTO outbox (aggregate_type, aggregate_id, event_type, event_payload, version, occurred_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                "Ticket",
                aggregateId,
                eventType,
                jsonb,
                version,
                Timestamp.from(occurredAt)
        );
    }

    private void insertUser(UUID userId, String username) {
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, true, 1)",
                userId,
                username,
                username + "@example.com",
                "Test " + username
        );
    }

    private void insertTicket(UUID ticketId, UUID requesterId, String ticketNumber, Instant createdAt) {
        // V6 migration removed ticket_number and updated allowed status values
        jdbcTemplate.update(
                "INSERT INTO tickets (id, title, status, priority, requester_id, created_at, updated_at, version, ticket_type) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?)",
                ticketId,
                "Seed Ticket",
                "NEW",
                "HIGH",
                requesterId,
                Timestamp.from(createdAt),
                Timestamp.from(createdAt),
                "INCIDENT"
        );
    }
}
