package io.monosense.synergyflow.eventing.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for OutboxEventEnvelope JSON serialization stability.
 */
class OutboxEventEnvelopeSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void envelopeSerializesWithStableFieldNames() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        String aggregateType = "TICKET";
        String eventType = "TicketCreated";
        Long version = 1L;
        Instant occurredAt = Instant.parse("2025-10-07T10:00:00Z");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ticket_id", aggregateId.toString());
        payload.put("title", "Test Ticket");

        OutboxEventEnvelope envelope = new OutboxEventEnvelope(
                aggregateId,
                aggregateType,
                eventType,
                version,
                occurredAt,
                payload
        );

        String json = objectMapper.writeValueAsString(envelope);
        JsonNode jsonNode = objectMapper.readTree(json);

        assertThat(jsonNode.has("aggregate_id")).isTrue();
        assertThat(jsonNode.has("aggregate_type")).isTrue();
        assertThat(jsonNode.has("event_type")).isTrue();
        assertThat(jsonNode.has("version")).isTrue();
        assertThat(jsonNode.has("occurred_at")).isTrue();
        assertThat(jsonNode.has("schema_version")).isTrue();
        assertThat(jsonNode.has("payload")).isTrue();
        assertThat(jsonNode.get("aggregate_id").asText()).isEqualTo(aggregateId.toString());
        assertThat(jsonNode.get("aggregate_type").asText()).isEqualTo(aggregateType);
        assertThat(jsonNode.get("event_type").asText()).isEqualTo(eventType);
        assertThat(jsonNode.get("version").asLong()).isEqualTo(version);
        assertThat(jsonNode.get("schema_version").asInt()).isEqualTo(OutboxEventEnvelope.CURRENT_SCHEMA_VERSION);
        assertThat(jsonNode.get("payload").get("title").asText()).isEqualTo("Test Ticket");
    }

    @Test
    void envelopeDeserializesCorrectly() throws Exception {
        String json = """
                {
                    "aggregate_id": "123e4567-e89b-12d3-a456-426614174000",
                    "aggregate_type": "ISSUE",
                    "event_type": "IssueStateChanged",
                    "version": 5,
                    "occurred_at": "2025-10-07T10:00:00Z",
                    "schema_version": 1,
                    "payload": {
                        "state": "IN_PROGRESS"
                    }
                }
                """;

        OutboxEventEnvelope envelope = objectMapper.readValue(json, OutboxEventEnvelope.class);

        assertThat(envelope.aggregateId()).isEqualTo(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));
        assertThat(envelope.aggregateType()).isEqualTo("ISSUE");
        assertThat(envelope.eventType()).isEqualTo("IssueStateChanged");
        assertThat(envelope.version()).isEqualTo(5L);
        assertThat(envelope.occurredAt()).isEqualTo(Instant.parse("2025-10-07T10:00:00Z"));
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.payload().get("state").asText()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void envelopeRoundTripPreservesData() throws Exception {
        UUID aggregateId = UUID.randomUUID();
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("field1", "value1");
        payload.put("field2", 42);

        OutboxEventEnvelope original = new OutboxEventEnvelope(
                aggregateId,
                "TICKET",
                "TicketAssigned",
                3L,
                Instant.now(),
                payload
        );

        String json = objectMapper.writeValueAsString(original);
        OutboxEventEnvelope deserialized = objectMapper.readValue(json, OutboxEventEnvelope.class);

        assertThat(deserialized.aggregateId()).isEqualTo(original.aggregateId());
        assertThat(deserialized.aggregateType()).isEqualTo(original.aggregateType());
        assertThat(deserialized.eventType()).isEqualTo(original.eventType());
        assertThat(deserialized.version()).isEqualTo(original.version());
        assertThat(deserialized.payload().get("field2").asInt()).isEqualTo(42);
    }
}

