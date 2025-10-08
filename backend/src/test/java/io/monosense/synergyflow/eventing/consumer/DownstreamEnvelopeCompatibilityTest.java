package io.monosense.synergyflow.eventing.consumer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contract tests that verify downstream consumers can process envelopes containing
 * the additional metadata introduced in Story 1.6 without breaking backward compatibility.
 */
class DownstreamEnvelopeCompatibilityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Test
    void sseConsumerIgnoresAdditionalEnvelopeMetadata() throws Exception {
        String payloadTitle = "Sample Ticket";
        String json = createEnvelopeJson(payloadTitle, "Agent Smith");

        SseEventEnvelope envelope = objectMapper.readValue(json, SseEventEnvelope.class);

        assertThat(envelope.aggregateId()).isNotNull();
        assertThat(envelope.eventType()).isEqualTo("TicketCreated");
        assertThat(envelope.payload().title()).isEqualTo(payloadTitle);
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.region()).isEqualTo("us-east-1");
    }

    @Test
    void searchIndexerConsumerDeserializesWithExpandedMetadata() throws Exception {
        String json = createEnvelopeJson("Sample Ticket", "Agent Smith");

        SearchIndexerEnvelope envelope = objectMapper.readValue(json, SearchIndexerEnvelope.class);

        assertThat(envelope.aggregateId()).isNotNull();
        assertThat(envelope.payload().assigneeName()).isEqualTo("Agent Smith");
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.additionalAttributes().get("delivery_tag").asText()).isEqualTo("redis-0-1");
    }

    private String createEnvelopeJson(String title, String assignee) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("aggregate_id", UUID.randomUUID().toString());
        root.put("aggregate_type", "Ticket");
        root.put("event_type", "TicketCreated");
        root.put("version", 3L);
        root.put("occurred_at", Instant.parse("2025-10-07T10:15:30Z").toString());
        root.put("schema_version", 1);
        root.put("region", "us-east-1");
        root.put("delivery_tag", "redis-0-1");

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("ticket_id", UUID.randomUUID().toString());
        payload.put("title", title);
        payload.put("assignee_name", assignee);
        payload.put("priority", "HIGH");
        root.set("payload", payload);

        ObjectNode attributes = objectMapper.createObjectNode();
        attributes.put("delivery_tag", "redis-0-1");
        attributes.put("shard", "primary");
        root.set("attributes", attributes);

        return root.toPrettyString();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SseEventEnvelope(
            @JsonProperty("aggregate_id") String aggregateId,
            @JsonProperty("aggregate_type") String aggregateType,
            @JsonProperty("event_type") String eventType,
            @JsonProperty("version") Long version,
            @JsonProperty("occurred_at") Instant occurredAt,
            @JsonProperty("schema_version") Integer schemaVersion,
            String region,
            Payload payload
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SearchIndexerEnvelope(
            @JsonProperty("aggregate_id") String aggregateId,
            @JsonProperty("aggregate_type") String aggregateType,
            @JsonProperty("event_type") String eventType,
            @JsonProperty("version") Long version,
            @JsonProperty("occurred_at") Instant occurredAt,
            @JsonProperty("schema_version") Integer schemaVersion,
            Payload payload,
            @JsonProperty("attributes") ObjectNode additionalAttributes
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Payload(
            @JsonProperty("ticket_id") String ticketId,
            String title,
            @JsonProperty("assignee_name") String assigneeName,
            String priority
    ) {
    }
}
