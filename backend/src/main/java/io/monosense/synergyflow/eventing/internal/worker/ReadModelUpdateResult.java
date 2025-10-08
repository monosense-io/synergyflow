package io.monosense.synergyflow.eventing.internal.worker;

/**
 * Result record representing the outcome of a read model update operation.
 *
 * <p>This immutable record provides a standardized way to track and report the results
 * of read model update operations performed by event handlers. It encapsulates both
 * the identity of the read model that was processed and whether the update was
 * actually applied or skipped due to various conditions.
 *
 * <p><strong>Update Scenarios:</strong>
 * <ul>
 *   <li><strong>Applied Updates:</strong> The read model was successfully updated with new data</li>
 *   <li><strong>Skipped Updates:</strong> The update was not applied due to conditions like:
 *     <ul>
 *       <li>Event already processed (idempotency)</li>
 *       <li>No relevant changes for this read model</li>
 *       <li>Event filtering criteria not met</li>
 *       <li>Read model not found for the event</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p><strong>Usage in Event Processing:</strong>
 * <p>This record is typically returned by event handler methods to indicate
 * the outcome of their processing:
 * <pre>{@code
 * @Component
 * public class TicketReadModelHandler {
 *
 *     public ReadModelUpdateResult handleTicketCreated(TicketCreatedEvent event) {
 *         TicketCardEntity entity = repository.findByTicketId(event.getTicketId());
 *
 *         if (entity != null) {
 *             // Already processed, skip
 *             return ReadModelUpdateResult.skipped("TicketCard");
 *         }
 *
 *         // Create and save new entity
 *         TicketCardEntity newEntity = mapper.toEntity(event);
 *         repository.save(newEntity);
 *
 *         return ReadModelUpdateResult.applied("TicketCard");
 *     }
 * }
 * }</pre>
 *
 * <p><strong>Monitoring and Metrics:</strong>
 * <p>These results can be aggregated for monitoring:
 * <ul>
 *   <li>Track processing success rates per read model type</li>
 *   <li>Identify models that are frequently skipped (potential optimization opportunities)</li>
 *   <li>Monitor event processing throughput and efficiency</li>
 *   <li>Debug issues with specific read model updates</li>
 * </ul>
 *
 * @param modelName the name of the read model that was processed (e.g., "TicketCard", "IssueCard")
 * @param updated whether the read model was actually updated (true) or the update was skipped (false)
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
record ReadModelUpdateResult(String modelName, boolean updated) {

    /**
     * Creates a result indicating that a read model was successfully updated.
     *
     * <p>This factory method should be used when the event handler successfully
     * applied changes to the read model, such as creating a new entity, updating
     * an existing one, or deleting an entity.
     *
     * @param modelName the name of the read model that was updated
     * @return a ReadModelUpdateResult indicating the update was applied
     * @throws IllegalArgumentException if modelName is null or blank
     */
    public static ReadModelUpdateResult applied(String modelName) {
        return new ReadModelUpdateResult(modelName, true);
    }

    /**
     * Creates a result indicating that a read model update was skipped.
     *
     * <p>This factory method should be used when the event handler determined
     * that no update was needed, such as when the event was already processed,
     * the read model doesn't apply to this event type, or no changes would result
     * from processing the event.
     *
     * @param modelName the name of the read model that was considered for update
     * @return a ReadModelUpdateResult indicating the update was skipped
     * @throws IllegalArgumentException if modelName is null or blank
     */
    public static ReadModelUpdateResult skipped(String modelName) {
        return new ReadModelUpdateResult(modelName, false);
    }
}
