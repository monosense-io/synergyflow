package io.monosense.synergyflow.common.events;

import java.time.Instant;

/**
 * Base Java record for all domain events in SynergyFlow.
 *
 * <p>All events MUST extend this record and include correlation/causation IDs
 * for distributed tracing. These IDs propagate via MDC and OpenTelemetry contexts.
 *
 * <h2>ID Semantics:</h2>
 * <ul>
 *   <li><b>Correlation ID:</b> Groups all events in the same logical transaction (e.g., single user request)</li>
 *   <li><b>Causation ID:</b> Points to the immediate event that caused this event (forms event chain)</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * public record IncidentCreatedEvent(
 *     String incidentId,
 *     String title,
 *     Priority priority,
 *     String correlationId,
 *     String causationId,
 *     Instant occurredAt
 * ) implements EventBase {}
 * </pre>
 *
 * @see CorrelationContext
 */
public interface EventBase {

    /**
     * Correlation ID groups events in the same logical transaction.
     * Inherited from incoming request or generated at entry point.
     */
    String correlationId();

    /**
     * Causation ID points to the event that directly caused this event.
     * For user-initiated events, equals correlationId.
     * For reactive events, this is the ID of the triggering event.
     */
    String causationId();

    /**
     * Timestamp when the event occurred (business time, not publication time).
     */
    Instant occurredAt();
}
