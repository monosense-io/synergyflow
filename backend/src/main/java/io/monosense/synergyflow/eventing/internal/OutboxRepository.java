package io.monosense.synergyflow.eventing.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for outbox event persistence and processing.
 *
 * <p>This repository manages the lifecycle of domain events stored in the transactional outbox.
 * It provides specialized queries for event processing workers to reliably retrieve, process,
 * and mark events as completed while supporting concurrent processing and gap detection.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Optimistic locking with FOR UPDATE SKIP LOCKED for concurrent processing</li>
 *   <li>Batch processing support for efficient event retrieval</li>
 *   <li>Gap detection for ensuring no events are missed during processing</li>
 *   <li>Progress tracking through processed event monitoring</li>
 * </ul>
 *
 * <p>This repository is package-private as it's only used within the eventing module
 * and should not be accessed directly by other modules. Use the EventPublisher interface
 * for publishing events and the worker infrastructure for processing.</p>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Finds a batch of unprocessed outbox events with optimistic locking.
     *
     * <p>This method uses PostgreSQL's {@code FOR UPDATE SKIP LOCKED} clause to safely retrieve
     * unprocessed events while allowing concurrent processing by multiple worker instances.
     * The SKIP LOCKED clause ensures that rows already locked by other workers are skipped,
     * enabling horizontal scaling of the event processing infrastructure.
     *
     * <p><strong>Concurrency Model:</strong>
     * <ul>
     *   <li>Multiple worker instances can process events concurrently</li>
     *   <li>Each worker gets a mutually exclusive set of events to process</li>
     *   <li>No two workers will process the same event simultaneously</li>
     *   <li>Processing continues even if some workers fail or are slow</li>
     * </ul>
     *
     * @param limit the maximum number of events to retrieve in this batch
     * @return list of unprocessed outbox events, ordered by ID for sequential processing
     */
    @Query(value = """
            SELECT *
            FROM outbox
            WHERE processed_at IS NULL
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findUnprocessedBatchForUpdate(@Param("limit") int limit);

    /**
     * Counts the number of unprocessed events in the outbox.
     *
     * <p>This method provides visibility into the current backlog of events that
     * need to be processed. It's useful for monitoring queue depth and detecting
     * potential processing bottlenecks or system issues.
     *
     * @return the count of events that have not yet been processed
     */
    long countByProcessedAtIsNull();

    /**
     * Marks an event as processed by setting the processed timestamp.
     *
     * <p>This method atomically updates the processed_at field for a specific event,
     * indicating that the event has been successfully published to the message broker
     * and can be considered completed. The update is performed with automatic flushing
     * to ensure the change is immediately visible to other transactions.
     *
     * <p><strong>Concurrency Considerations:</strong>
     * <ul>
     *   <li>This method should be called within the worker's transaction context</li>
     *   <li>The event row should be locked via {@link #findUnprocessedBatchForUpdate(int)}</li>
     *   <li>Only one worker should process a given event simultaneously</li>
     * </ul>
     *
     * @param id the primary key of the event to mark as processed
     * @param processedAt the timestamp when the event was successfully processed
     * @return the number of rows affected (should be 1 if successful, 0 if not found)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OutboxEvent e SET e.processedAt = :processedAt WHERE e.id = :id")
    int markProcessed(@Param("id") Long id, @Param("processedAt") Instant processedAt);

    /**
     * Finds the highest ID of successfully processed events.
     *
     * <p>This method is used by the gap detection mechanism to determine the
     * current processing progress. It represents the highest event ID that has
     * been successfully processed and published to the message broker.
     *
     * <p><strong>Gap Detection Usage:</strong>
     * <p>This value is compared against {@link #findCurrentMaxId()} to identify
     * potential gaps in event processing. If there are unprocessed events with
     * IDs less than this maximum, it indicates a processing gap that needs
     * to be investigated and resolved.
     *
     * @return the maximum ID of processed events, or null if no events have been processed
     */
    @Query("SELECT MAX(e.id) FROM OutboxEvent e WHERE e.processedAt IS NOT NULL")
    Long findMaxProcessedId();

    /**
     * Finds the highest ID among all events in the outbox table.
     *
     * <p>This method returns the maximum event ID currently stored in the outbox,
     * regardless of processing status. It's used by the gap detection mechanism
     * to understand the total event volume and identify potential processing gaps.
     *
     * <p><strong>Gap Detection Usage:</strong>
     * <p>This value is compared against {@link #findMaxProcessedId()} to determine
     * if there are events that should have been processed but weren't. A significant
     * difference between the current max and processed max indicates a potential
     * processing issue that needs attention.
     *
     * @return the maximum event ID in the table, or null if the table is empty
     */
    @Query("SELECT MAX(e.id) FROM OutboxEvent e")
    Long findCurrentMaxId();

    /**
     * Finds IDs of unprocessed events up to a given maximum ID for gap detection.
     *
     * <p>This method is the core of the gap detection mechanism. It identifies events
     * that should have been processed (based on their ID being less than or equal to
     * the maximum processed ID) but remain unprocessed. These represent gaps in the
     * event processing pipeline that need to be investigated and potentially retried.
     *
     * <p><strong>Gap Detection Process:</strong>
     * <ol>
     *   <li>Call {@link #findMaxProcessedId()} to get the highest processed ID</li>
     *   <li>Call this method with that ID to find unprocessed events up to that point</li>
     *   <li>If the returned list is not empty, there are gaps that need attention</li>
     *   <li>The gap events can be manually retried or investigated for issues</li>
     * </ol>
     *
     * <p><strong>Common Gap Causes:</strong>
     * <ul>
     *   <li>Worker crashes during event processing</li>
     *   <li>Network timeouts when publishing to message broker</li>
     *   <li>Database connectivity issues during status updates</li>
     *   <li>Transient errors that prevent successful processing</li>
     * </ul>
     *
     * @param maxId the maximum event ID to check for gaps (typically from findMaxProcessedId)
     * @return list of event IDs that represent gaps in processing, ordered by ID
     */
    @Query("SELECT e.id FROM OutboxEvent e WHERE e.id <= :maxId AND e.processedAt IS NULL ORDER BY e.id")
    List<Long> findGapIdsUpTo(@Param("maxId") Long maxId);
}
