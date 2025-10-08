package io.monosense.synergyflow.eventing.internal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Envelope record for JSON serialization with stable field names and schema versioning.
 *
 * <p>This immutable record represents the standardized envelope format for all domain events
 * stored in the outbox table's {@code event_payload} column. It provides a consistent structure
 * for event serialization and deserialization across different event types.
 *
 * <p><strong>Envelope Structure:</strong>
 * <ul>
 *   <li><strong>Metadata Fields:</strong> Aggregate identification, event type, version, and timestamps</li>
 *   <li><strong>Schema Versioning:</strong> Supports evolution of event structure over time</li>
 *   <li><strong>Stable Naming:</strong> Uses {@code lowercase_with_underscores} for backward compatibility</li>
 *   <li><strong>Type Safety:</strong> Strongly typed fields with proper JSON annotations</li>
 * </ul>
 *
 * <p><strong>Field Descriptions:</strong>
 * <ul>
 *   <li>{@code aggregate_id}: Unique identifier of the aggregate that generated the event</li>
 *   <li>{@code aggregate_type}: Type name of the aggregate (e.g., "Ticket", "Issue")</li>
 *   <li>{@code event_type}: Specific event type (e.g., "TicketCreated", "IssueStateChanged")</li>
 *   <li>{@code version}: Aggregate version for ordering and idempotency</li>
 *   <li>{@code occurred_at}: Timestamp when the event occurred in the domain</li>
 *   <li>{@code schema_version}: Version of the envelope structure for compatibility</li>
 *   <li>{@code payload}: The actual event data as JSON node</li>
 * </ul>
 *
 * <p><strong>Schema Versioning:</strong>
 * <p>The envelope includes schema versioning to support evolution of event structure:
 * <ul>
 *   <li>Version 1: Initial envelope structure</li>
 *   <li>Future versions can add new fields while maintaining compatibility</li>
 *   <li>Consumers can check schema version to handle different envelope formats</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // Create envelope for a ticket created event
 * ObjectNode payload = objectMapper.createObjectNode();
 * payload.put("ticketId", ticketId.toString());
 * payload.put("title", "Server Down");
 * payload.put("priority", "HIGH");
 *
 * OutboxEventEnvelope envelope = new OutboxEventEnvelope(
 *     ticketId,
 *     "Ticket",
 *     "TicketCreated",
 *     1L,
 *     Instant.now(),
 *     payload
 * );
 *
 * // Serialize to JSON for storage
 * String json = objectMapper.writeValueAsString(envelope);
 * }</pre>
 *
 * <p><strong>JSON Format:</strong>
 * <pre>{@code
 * {
 *   "aggregate_id": "123e4567-e89b-12d3-a456-426614174000",
 *   "aggregate_type": "Ticket",
 *   "event_type": "TicketCreated",
 *   "version": 1,
 *   "occurred_at": "2025-01-01T10:00:00Z",
 *   "schema_version": 1,
 *   "payload": {
 *     "ticketId": "123e4567-e89b-12d3-a456-426614174000",
 *     "title": "Server Down",
 *     "priority": "HIGH"
 *   }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see OutboxEvent
 * @see OutboxRepository
 * @see OutboxPoller
 */
record OutboxEventEnvelope(
        @JsonProperty("aggregate_id") UUID aggregateId,
        @JsonProperty("aggregate_type") String aggregateType,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("version") Long version,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("schema_version") Integer schemaVersion,
        @JsonProperty("payload") JsonNode payload
) {

    /**
     * Current schema version for the envelope structure.
     *
     * <p>This constant defines the version of the envelope format currently in use.
     * It enables consumers to handle different envelope versions gracefully as the
     * structure evolves over time.
     */
    public static final int CURRENT_SCHEMA_VERSION = 1;

    /**
     * Convenience constructor that uses the current schema version.
     *
     * <p>This constructor creates an envelope using {@link #CURRENT_SCHEMA_VERSION}
     * for the schema version field. This is the preferred constructor for creating
     * new events in the current system version.
     *
     * @param aggregateId the unique identifier of the aggregate that generated the event
     * @param aggregateType the type name of the aggregate (e.g., "Ticket", "Issue")
     * @param eventType the specific event type (e.g., "TicketCreated", "IssueStateChanged")
     * @param version the aggregate version for ordering and idempotency
     * @param occurredAt the timestamp when the event occurred in the domain
     * @param payload the actual event data as JSON node
     * @throws IllegalArgumentException if any required parameter is null
     */
    public OutboxEventEnvelope(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Long version,
            Instant occurredAt,
            JsonNode payload) {
        this(aggregateId, aggregateType, eventType, version, occurredAt, CURRENT_SCHEMA_VERSION, payload);
    }
}
