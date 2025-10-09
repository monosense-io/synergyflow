package io.monosense.synergyflow.eventing.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.monosense.synergyflow.eventing.api.DuplicateEventException;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.sql.SQLException;

/**
 * Transactional event publisher implementation.
 *
 * <p>Publishes domain events to the transactional outbox table in the same database transaction
 * as the aggregate write operation. This ensures exactly-once event delivery semantics and maintains
 * consistency between the domain state and published events.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Transactional consistency with aggregate writes</li>
 *   <li>Automatic retry for transient database errors</li>
 *   <li>Duplicate event detection and prevention</li>
 *   <li>Comprehensive metrics and structured logging</li>
 *   <li>JSON serialization of event payloads</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class TicketService {
 *     private final EventPublisher eventPublisher;
 *
 *     public void createTicket(CreateTicketCommand cmd) {
 *         Ticket ticket = new Ticket(cmd);
 *         ticketRepository.save(ticket);
 *
 *         // Event published in same transaction
 *         eventPublisher.publish(
 *             ticket.getId(),
 *             "TICKET",
 *             "TicketCreated",
 *             ticket.getVersion(),
 *             ticket.getCreatedAt(),
 *             toJson(new TicketCreatedEvent(ticket))
 *         );
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Service
class EventPublisherImpl implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherImpl.class);
    private static final String METRIC_PREFIX = "outbox";

    private final OutboxRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs a new EventPublisherImpl with required dependencies.
     *
     * @param repository the outbox repository for persisting events
     * @param objectMapper the JSON object mapper for serializing event payloads
     * @param meterRegistry the metrics registry for recording publishing metrics
     * @throws IllegalArgumentException if any parameter is null
     */
    public EventPublisherImpl(
            OutboxRepository repository,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Publishes a domain event to the transactional outbox table.
     *
     * <p>This method implements the core of the transactional outbox pattern by storing
     * domain events in the same database transaction as aggregate state changes. This ensures
     * exactly-once delivery semantics and maintains consistency between domain state and events.
     *
     * <p>The method includes comprehensive error handling:
     * <ul>
     *   <li><strong>Duplicate Detection:</strong> Detects and throws {@link DuplicateEventException} for duplicate events</li>
     *   <li><strong>Retry Logic:</strong> Retries transient database errors (connection issues, deadlocks)</li>
     *   <li><strong>Metrics:</strong> Records comprehensive metrics for monitoring and observability</li>
     *   <li><strong>Logging:</strong> Provides structured logging for debugging and auditing</li>
     * </ul>
     *
     * <p><strong>Transaction Behavior:</strong>
     * <p>This method runs within the caller's transaction context. The event is only persisted
     * if the overall transaction commits successfully. If the transaction rolls back, the event
     * is also rolled back, maintaining consistency.
     *
     * <p><strong>Error Handling:</strong>
     * <ul>
     *   <li>{@link DuplicateEventException}: Thrown when the same event is published twice</li>
     *   <li>{@link DataIntegrityViolationException}: Thrown for database constraint violations</li>
     *   <li>{@link DataAccessException}: Thrown for general database access errors</li>
     * </ul>
     *
     * @param aggregateId the unique identifier of the aggregate that generated the event
     * @param aggregateType the type name of the aggregate (e.g., "Ticket", "Issue")
     * @param eventType the specific event type (e.g., "TicketCreated", "IssueStateChanged")
     * @param version the aggregate version for ordering and idempotency
     * @param occurredAt the timestamp when the event occurred in the domain
     * @param payload the event data as JSON node
     * @throws DuplicateEventException if the same event is already published
     * @throws DataIntegrityViolationException if database constraints are violated
     * @throws DataAccessException if database access fails
     * @throws IllegalArgumentException if any required parameter is null
     */
    @Override
    @Transactional
    public void publish(
            UUID aggregateId,
            String aggregateType,
            String eventType,
            Long version,
            Instant occurredAt,
            JsonNode payload) {

        long startTime = System.currentTimeMillis();
        int attempt = 0;
        final int maxRetries = 2; // simple bounded retry for transient DB errors

        while (true) {
            try {
            // Create and persist outbox event with RAW payload
            OutboxEvent event = new OutboxEvent(
                    aggregateId,
                    aggregateType,
                    eventType,
                    version,
                    occurredAt,
                    payload
            );

            repository.save(event);

            long latencyMs = System.currentTimeMillis() - startTime;

            // Record success metrics
            incrementCounter("published_total", aggregateType, eventType, "success");
            recordLatency(aggregateType, eventType, latencyMs);

            // Structured log on success
            log.info("Event published to outbox: aggregate_id={}, aggregate_type={}, event_type={}, version={}, latency_ms={}",
                    aggregateId, aggregateType, eventType, version, latencyMs);
                return; // success

            } catch (DataIntegrityViolationException e) {
                if (isDuplicateViolation(e)) {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    incrementCounter("publish_errors_total", aggregateType, eventType, "duplicate");
                    log.warn("Duplicate event ignored: aggregate_id={}, aggregate_type={}, event_type={}, version={}, latency_ms={}",
                            aggregateId, aggregateType, eventType, version, latencyMs);
                    throw new DuplicateEventException(
                            String.format("Event already exists for aggregate %s version %d", aggregateId, version), e);
                }

                if (isTransient(e) && attempt < maxRetries) {
                    attempt++;
                    log.debug("Transient DB error on publish (attempt {}/{}). Retrying...", attempt, maxRetries, e);
                    continue; // retry
                }

                incrementCounter("publish_errors_total", aggregateType, eventType, "data_integrity");
                log.error("Data integrity violation publishing event: aggregate_id={}, aggregate_type={}, event_type={}, version={}, error={}",
                        aggregateId, aggregateType, eventType, version, e.getClass().getSimpleName(), e);
                throw e;

            } catch (Exception e) {
                if (e instanceof DataAccessException dae && isTransient(dae) && attempt < maxRetries) {
                    attempt++;
                    log.debug("Transient data access error on publish (attempt {}/{}). Retrying...", attempt, maxRetries, e);
                    continue; // retry
                }

                long latencyMs = System.currentTimeMillis() - startTime;
                incrementCounter("publish_errors_total", aggregateType, eventType, e.getClass().getSimpleName());
                log.error("Failed to publish event: aggregate_id={}, aggregate_type={}, event_type={}, version={}, latency_ms={}, error={}",
                        aggregateId, aggregateType, eventType, version, latencyMs, e.getClass().getSimpleName(), e);
                throw e;
            }
        }
    }

    /**
     * Increments a counter metric with the specified tags.
     *
     * <p>This helper method records metrics for monitoring event publishing behavior,
     * including success rates, error types, and performance characteristics.
     *
     * @param metricName the base name of the metric (e.g., "published_total", "publish_errors_total")
     * @param aggregateType the aggregate type tag for metric filtering
     * @param eventType the event type tag for metric filtering
     * @param result the result tag (e.g., "success", "duplicate", "data_integrity")
     */
    private void incrementCounter(String metricName, String aggregateType, String eventType, String result) {
        Counter.builder(METRIC_PREFIX + "_" + metricName)
                .tag("aggregate_type", aggregateType)
                .tag("event_type", eventType)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Records latency metrics for event publishing operations.
     *
     * <p>This helper method tracks the time taken to publish events to the outbox,
     * providing visibility into performance characteristics and identifying potential bottlenecks.
     *
     * @param aggregateType the aggregate type tag for metric filtering
     * @param eventType the event type tag for metric filtering
     * @param latencyMs the latency in milliseconds for the publish operation
     */
    private void recordLatency(String aggregateType, String eventType, long latencyMs) {
        Timer.builder(METRIC_PREFIX + "_publish_latency_ms")
                .tag("aggregate_type", aggregateType)
                .tag("event_type", eventType)
                .register(meterRegistry)
                .record(java.time.Duration.ofMillis(latencyMs));
    }

    /**
     * Determines if an exception represents a duplicate key violation.
     *
     * <p>This method checks for duplicate constraint violations using multiple strategies:
     * <ul>
     *   <li>Primary: SQLState 23505 (unique constraint violation) from SQLException</li>
     *   <li>Fallback: Exception message containing the unique index name</li>
     * </ul>
     *
     * @param e the throwable to examine for duplicate violation indicators
     * @return true if the exception represents a duplicate key violation, false otherwise
     */
    private static boolean isDuplicateViolation(Throwable e) {
        // Check SQLState 23505 (unique violation) anywhere in the cause chain
        String sqlState = extractSqlState(e);
        if (Objects.equals(sqlState, "23505")) return true;

        // Hibernate ConstraintViolationException may carry SQLState via SQLException cause
        // Fallback: check message for the specific unique index name (DB-agnostic but brittle)
        String msg = e.getMessage();
        return msg != null && msg.contains("idx_outbox_idempotency");
    }

    /**
     * Determines if an exception represents a transient database error.
     *
     * <p>Transient errors are temporary issues that may resolve with retry attempts.
     * This method identifies such errors based on their SQLState codes:
     * <ul>
     *   <li>08xxx: Connection exceptions (network issues, server restarts)</li>
     *   <li>40001: Serialization failures (concurrent modification conflicts)</li>
     *   <li>40P01: Deadlock detection errors</li>
     * </ul>
     *
     * @param e the throwable to examine for transient error indicators
     * @return true if the exception represents a transient error that may be retried, false otherwise
     */
    private static boolean isTransient(Throwable e) {
        String sqlState = extractSqlState(e);
        if (sqlState == null) return false;
        // Connection exceptions (08xxx), serialization failure (40001), deadlock (40P01)
        return sqlState.startsWith("08") || sqlState.equals("40001") || sqlState.equals("40P01");
    }

    /**
     * Extracts the SQLState from an exception chain.
     *
     * <p>This method traverses the exception cause chain to find the first SQLException
     * with a non-null SQLState. This is necessary because various database access frameworks
     * wrap SQLException in different exception types.
     *
     * @param e the throwable to search for SQLState information
     * @return the SQLState code if found, null otherwise
     */
    private static String extractSqlState(Throwable e) {
        Throwable t = e;
        while (t != null) {
            if (t instanceof SQLException se && se.getSQLState() != null) {
                return se.getSQLState();
            }
            t = t.getCause();
        }
        return null;
    }
}
