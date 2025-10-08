package io.monosense.synergyflow.eventing.internal;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Transactional outbox event entity for reliable event delivery.
 *
 * <p>This JPA entity implements the transactional outbox pattern to ensure reliable event delivery
 * in distributed systems. Domain events are stored in the same database transaction as aggregate
 * state changes, guaranteeing that events are not lost if the application crashes after the
 * business logic completes but before the event is processed.
 *
 * <p><strong>Outbox Pattern Benefits:</strong>
 * <ul>
 *   <li><strong>Atomicity:</strong> Events and aggregate state changes are saved in the same transaction</li>
 *   <li><strong>Reliability:</strong> No events are lost due to application crashes or network failures</li>
 *   <li><strong>Ordering:</strong> Events are processed in the same order they were created</li>
 *   <li><strong>Retry Logic:</strong> Failed events can be retried with exponential backoff</li>
 *   <li><strong>Idempotency:</strong> Version tracking prevents duplicate event processing</li>
 * </ul>
 *
 * <p><strong>Event Lifecycle:</strong>
 * <ol>
 *   <li>Domain event occurs during business logic execution</li>
 *   <li>Event is serialized and stored in outbox table with aggregate state changes</li>
 *   <li>Worker process polls outbox table for unprocessed events</li>
 *   <li>Event is published to message broker (Redis streams)</li>
 *   <li>Event is marked as processed or retry count is incremented on failure</li>
 * </ol>
 *
 * <p><strong>Schema Details:</strong>
 * <ul>
 *   <li>{@code id}: Auto-generated primary key for internal tracking</li>
 *   <li>{@code aggregate_id}: ID of the aggregate that generated the event</li>
 *   <li>{@code aggregate_type}: Type of aggregate (e.g., "Ticket", "Issue")</li>
 *   <li>{@code event_type}: Specific event type (e.g., "TicketCreated", "IssueStateChanged")</li>
 *   <li>{@code version}: Aggregate version for ordering and idempotency</li>
 *   <li>{@code occurred_at}: Timestamp when the event occurred in the domain</li>
 *   <li>{@code event_payload}: JSON serialized event data</li>
 *   <li>{@code processed_at}: Timestamp when the event was successfully processed</li>
 *   <li>{@code processing_error}: Error details if processing failed</li>
 *   <li>{@code retry_count}: Number of processing attempts made</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class TicketService {
 *     @Autowired
 *     private OutboxRepository outboxRepository;
 *
 *     @Transactional
 *     public Ticket createTicket(CreateTicketCommand command) {
 *         Ticket ticket = new Ticket(command);
 *         ticketRepository.save(ticket);
 *
 *         // Store event in outbox within same transaction
 *         TicketCreated event = new TicketCreated(ticket.getId(), ticket.getDetails());
 *         OutboxEvent outboxEvent = new OutboxEvent(
 *             ticket.getId(),
 *             "Ticket",
 *             "TicketCreated",
 *             ticket.getVersion(),
 *             Instant.now(),
 *             event.toJson()
 *         );
 *         outboxRepository.save(outboxEvent);
 *
 *         return ticket;
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see OutboxRepository
 * @see OutboxEventEnvelope
 * @see OutboxPoller
 */
@Entity
@Table(name = "outbox")
class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "event_payload", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private JsonNode eventPayload;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    /**
     * Default constructor required by JPA.
     *
     * <p>This protected constructor is used by JPA/Hibernate for entity instantiation.
     * Application code should use the full constructor to create OutboxEvent instances.
     */
    protected OutboxEvent() {
        // JPA requires a no-arg constructor
    }

    /**
     * Creates a new OutboxEvent instance for storing a domain event.
     *
     * <p>This constructor creates an outbox event entry that will be processed by the worker
     * to publish the event to the message broker. The event is created in an unprocessed state
     * with retry count initialized to 0.
     *
     * @param aggregateId the unique identifier of the aggregate that generated the event
     * @param aggregateType the type of aggregate (e.g., "Ticket", "Issue")
     * @param eventType the specific event type (e.g., "TicketCreated", "IssueStateChanged")
     * @param version the aggregate version for ordering and idempotency
     * @param occurredAt the timestamp when the event occurred in the domain
     * @param eventPayload the JSON serialized event data
     * @throws IllegalArgumentException if any required parameter is null
     */
    OutboxEvent(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Long version,
            Instant occurredAt,
            JsonNode eventPayload) {

        if (aggregateId == null || aggregateType == null || eventType == null
                || version == null || occurredAt == null || eventPayload == null) {
            throw new IllegalArgumentException("OutboxEvent constructor received null argument");
        }

        this.aggregateId = aggregateId;
        this.aggregateType = aggregateType;
        this.eventType = eventType;
        this.version = version;
        this.occurredAt = occurredAt;
        this.eventPayload = eventPayload;
        this.retryCount = 0;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getEventType() {
        return eventType;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public JsonNode getEventPayload() {
        return eventPayload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public String getProcessingError() {
        return processingError;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

}
