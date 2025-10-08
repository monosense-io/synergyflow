package io.monosense.synergyflow.eventing.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * A JSON envelope that represents a domain event exported by the transactional outbox pattern.
 * <p>
 * This record encapsulates all the necessary metadata and payload information required for
 * reliable event delivery and processing. It serves as a standardized format for events
 * that are persisted in the outbox table and later processed by the event publishing system.
 * </p>
 * <p>
 * The envelope includes:
 * <ul>
 *     <li>Aggregate identification (ID and type)</li>
 *     <li>Event type and version information</li>
 *     <li>Timestamp and schema version</li>
 *     <li>The actual event payload as JSON</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * OutboxEnvelope envelope = new OutboxEnvelope(
 *     UUID.randomUUID(),
 *     "Issue",
 *     "IssueCreated",
 *     1L,
 *     Instant.now(),
 *     1,
 *     objectMapper.valueToTree(eventPayload)
 * );
 * </pre>
 * </p>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
public record OutboxEnvelope(
        @JsonProperty("aggregate_id") UUID aggregateId,
        @JsonProperty("aggregate_type") String aggregateType,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("version") Long version,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("schema_version") Integer schemaVersion,
        @JsonProperty("payload") JsonNode payload
) {
}
