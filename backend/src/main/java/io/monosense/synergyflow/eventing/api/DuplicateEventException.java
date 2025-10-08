package io.monosense.synergyflow.eventing.api;

/**
 * Exception thrown when attempting to publish a duplicate event.
 *
 * <p>This occurs when an event with the same (aggregateId, version) already exists in the outbox.
 * This exception is part of the idempotency guarantee provided by the transactional outbox pattern,
 * preventing duplicate events from being published for the same aggregate state change.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * try {
 *     eventPublisher.publish(aggregateId, aggregateType, eventType, version, occurredAt, payload);
 * } catch (DuplicateEventException e) {
 *     // This is expected behavior for retries - the event was already published
 *     log.debug("Duplicate event publication ignored: {}", e.getMessage());
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
public class DuplicateEventException extends RuntimeException {

    /**
     * Constructs a new DuplicateEventException with the specified detail message.
     *
     * @param message the detail message explaining the duplicate event situation
     */
    public DuplicateEventException(String message) {
        super(message);
    }

    /**
     * Constructs a new DuplicateEventException with the specified detail message and cause.
     *
     * @param message the detail message explaining the duplicate event situation
     * @param cause the underlying cause (typically a DataIntegrityViolationException)
     */
    public DuplicateEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
