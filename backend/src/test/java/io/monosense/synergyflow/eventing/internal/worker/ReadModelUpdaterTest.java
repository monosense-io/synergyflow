package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ReadModelUpdaterTest {

    private ObjectMapper objectMapper;
    private TicketReadModelHandler ticketReadModelHandler;
    private IssueReadModelHandler issueReadModelHandler;
    private SimpleMeterRegistry meterRegistry;
    private ReadModelUpdater readModelUpdater;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        ticketReadModelHandler = mock(TicketReadModelHandler.class);
        issueReadModelHandler = mock(IssueReadModelHandler.class);
        meterRegistry = new SimpleMeterRegistry();
        readModelUpdater = new ReadModelUpdater(objectMapper, ticketReadModelHandler, issueReadModelHandler, meterRegistry);
    }

    @Test
    void routesTicketCreatedEvents() {
        UUID aggregateId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ticketId", aggregateId.toString());
        payload.put("ticketNumber", "T-123");
        payload.put("title", "Ticket created");
        payload.put("status", "OPEN");
        payload.put("priority", "HIGH");
        payload.put("requesterId", UUID.randomUUID().toString());
        payload.put("createdAt", Instant.now().toString());
        payload.put("updatedAt", Instant.now().toString());

        OutboxMessage event = new OutboxMessage(1L, aggregateId, "TICKET", "TicketCreated", 1L, Instant.now(), payload);

        when(ticketReadModelHandler.handleTicketCreated(any(TicketCreatedMessage.class), eq(1L)))
                .thenReturn(ReadModelUpdateResult.applied("ticket_card"));

        readModelUpdater.update(event);

        verify(ticketReadModelHandler).handleTicketCreated(any(TicketCreatedMessage.class), eq(1L));
        double total = meterRegistry.find("outbox_read_model_updates_total").counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
        assertThat(total).isEqualTo(1.0);
    }

    @Test
    void routesIssueCreatedEvents() {
        UUID issueId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("issueId", issueId.toString());
        payload.put("issueKey", "ISS-1");
        payload.put("title", "Issue created");
        payload.put("issueType", "BUG");
        payload.put("status", "TODO");
        payload.put("priority", "MEDIUM");
        payload.put("reporterId", UUID.randomUUID().toString());
        payload.put("createdAt", Instant.now().toString());
        payload.put("updatedAt", Instant.now().toString());

        OutboxMessage event = new OutboxMessage(42L, issueId, "ISSUE", "IssueCreated", 1L, Instant.now(), payload);

        when(issueReadModelHandler.handleIssueCreated(any(IssueCreatedMessage.class), eq(1L)))
                .thenReturn(List.of(ReadModelUpdateResult.applied("issue_card"), ReadModelUpdateResult.applied("queue_row")));

        readModelUpdater.update(event);

        verify(issueReadModelHandler).handleIssueCreated(any(IssueCreatedMessage.class), eq(1L));
        double total = meterRegistry.find("outbox_read_model_updates_total").counters().stream()
                .mapToDouble(counter -> counter.count())
                .sum();
        assertThat(total).isEqualTo(2.0);
    }

    @Test
    void recordsErrorMetricWhenHandlerThrows() {
        UUID aggregateId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ticketId", aggregateId.toString());
        payload.put("ticketNumber", "T-123");
        payload.put("title", "Ticket created");
        payload.put("status", "OPEN");
        payload.put("priority", "HIGH");
        payload.put("requesterId", UUID.randomUUID().toString());
        payload.put("createdAt", Instant.now().toString());
        payload.put("updatedAt", Instant.now().toString());

        OutboxMessage event = new OutboxMessage(7L, aggregateId, "TICKET", "TicketCreated", 1L, Instant.now(), payload);

        when(ticketReadModelHandler.handleTicketCreated(any(TicketCreatedMessage.class), eq(1L)))
                .thenThrow(new IllegalStateException("boom"));

        readModelUpdater.update(event);

        assertThat(meterRegistry.get("outbox_read_model_update_errors_total").counter().count()).isEqualTo(1.0);
    }

    private ObjectNode buildEnvelope(UUID aggregateId, String eventType, long version, ObjectNode payload) {
        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("aggregate_id", aggregateId.toString());
        envelope.put("aggregate_type", "TEST");
        envelope.put("event_type", eventType);
        envelope.put("version", version);
        envelope.put("occurred_at", Instant.now().toString());
        envelope.put("schema_version", 1);
        envelope.set("payload", payload);
        return envelope;
    }
}
