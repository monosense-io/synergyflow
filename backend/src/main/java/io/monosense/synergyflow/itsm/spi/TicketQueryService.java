package io.monosense.synergyflow.itsm.spi;

import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service Provider Interface for cross-module ticket queries.
 *
 * <p>This interface defines a stable contract for querying ticket data from other bounded contexts
 * within the SynergyFlow application (PM, Workflow, Dashboard modules). It provides read-only access
 * to ticket information without coupling consumers to internal entity implementation details.</p>
 *
 * <p><strong>Module Boundary:</strong> This interface is annotated with {@link NamedInterface}
 * to explicitly mark it as a cross-module boundary in the Spring Modulith architecture. Modules
 * outside the ITSM bounded context MUST access ticket data exclusively through this SPI.</p>
 *
 * <p><strong>SPI Stability Contract:</strong> This interface is part of the public API and follows
 * semantic versioning:</p>
 * <ul>
 *   <li><strong>Minor versions</strong> may add new query methods (backward compatible)</li>
 *   <li><strong>Major versions</strong> required for removing or changing existing method signatures</li>
 *   <li>Deprecation cycle: Methods marked {@code @Deprecated} for one major version before removal</li>
 * </ul>
 *
 * <p><strong>Implementation Note:</strong> The internal implementation ({@code TicketQueryServiceImpl})
 * is package-private in {@code internal.service} and uses {@code @Transactional(readOnly=true)} for
 * optimized database access.</p>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>
 * // PM Module: Link task to ticket
 * &#64;Autowired
 * private TicketQueryService ticketQueryService;
 *
 * Optional&lt;TicketSummaryDTO&gt; ticket = ticketQueryService.findById(ticketId);
 * if (ticket.isPresent()) {
 *     task.linkToTicket(ticket.get().id());
 * }
 *
 * // Dashboard Module: Aggregate metrics
 * long openTickets = ticketQueryService.countByStatus(TicketStatus.NEW);
 * List&lt;TicketSummaryDTO&gt; criticalTickets = ticketQueryService.findByPriority(Priority.CRITICAL);
 * </pre>
 *
 * @author monosense
 * @since 2.2
 */
@NamedInterface("TicketQueryService")
public interface TicketQueryService {

    /**
     * Finds a ticket by its unique identifier.
     *
     * @param ticketId the UUID of the ticket to find
     * @return an Optional containing the TicketSummaryDTO if found, or empty if not found
     * @throws IllegalArgumentException if ticketId is null
     * @since 2.2
     */
    Optional<TicketSummaryDTO> findById(UUID ticketId);

    /**
     * Finds all tickets assigned to a specific agent.
     *
     * <p>Returns tickets where the assigneeId matches the provided agent UUID, ordered by
     * creation time (newest first).</p>
     *
     * @param assigneeId the UUID of the agent to filter by
     * @return list of tickets assigned to the agent, or empty list if none found
     * @throws IllegalArgumentException if assigneeId is null
     * @since 2.2
     */
    List<TicketSummaryDTO> findByAssignee(UUID assigneeId);

    /**
     * Finds all tickets created by a specific requester.
     *
     * <p>Returns tickets where the requesterId matches the provided user UUID, ordered by
     * creation time (newest first).</p>
     *
     * @param requesterId the UUID of the requester to filter by
     * @return list of tickets created by the requester, or empty list if none found
     * @throws IllegalArgumentException if requesterId is null
     * @since 2.2
     */
    List<TicketSummaryDTO> findByRequester(UUID requesterId);

    /**
     * Finds all tickets with a specific status.
     *
     * <p>Returns tickets matching the provided status, ordered by creation time (newest first).</p>
     *
     * @param status the ticket status to filter by (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
     * @return list of tickets with the specified status, or empty list if none found
     * @throws IllegalArgumentException if status is null
     * @since 2.2
     */
    List<TicketSummaryDTO> findByStatus(TicketStatus status);

    /**
     * Finds all tickets with a specific priority level.
     *
     * <p>Returns tickets matching the provided priority, ordered by creation time (newest first).</p>
     *
     * @param priority the priority level to filter by (CRITICAL, HIGH, MEDIUM, LOW)
     * @return list of tickets with the specified priority, or empty list if none found
     * @throws IllegalArgumentException if priority is null
     * @since 2.2
     */
    List<TicketSummaryDTO> findByPriority(Priority priority);

    /**
     * Finds tickets matching complex search criteria.
     *
     * <p>Performs a dynamic query based on the provided criteria. Only non-empty Optional fields
     * in the criteria are used as filters. All criteria are combined with AND logic.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * TicketSearchCriteria criteria = TicketSearchCriteria.builder()
     *     .status(TicketStatus.ASSIGNED)
     *     .priority(Priority.HIGH)
     *     .assigneeId(agentUuid)
     *     .build();
     * List&lt;TicketSummaryDTO&gt; tickets = ticketQueryService.findByCriteria(criteria);
     * </pre>
     *
     * @param criteria the search criteria with optional filters
     * @return list of tickets matching all specified criteria, or empty list if none found
     * @throws IllegalArgumentException if criteria is null
     * @since 2.2
     */
    List<TicketSummaryDTO> findByCriteria(TicketSearchCriteria criteria);

    /**
     * Checks if a ticket exists with the specified identifier.
     *
     * <p>Optimized existence check without loading the full entity.</p>
     *
     * @param ticketId the UUID of the ticket to check
     * @return true if a ticket with the ID exists, false otherwise
     * @throws IllegalArgumentException if ticketId is null
     * @since 2.2
     */
    boolean existsById(UUID ticketId);

    /**
     * Counts tickets with a specific status.
     *
     * <p>Optimized count query without loading entities.</p>
     *
     * @param status the ticket status to count (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
     * @return the number of tickets with the specified status
     * @throws IllegalArgumentException if status is null
     * @since 2.2
     */
    long countByStatus(TicketStatus status);

    /**
     * Counts tickets assigned to a specific agent.
     *
     * <p>Optimized count query without loading entities.</p>
     *
     * @param assigneeId the UUID of the agent to count tickets for
     * @return the number of tickets assigned to the agent
     * @throws IllegalArgumentException if assigneeId is null
     * @since 2.2
     */
    long countByAssignee(UUID assigneeId);
}
