package io.monosense.synergyflow.itsm.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import io.monosense.synergyflow.itsm.api.dto.AddCommentCommand;
import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.events.TicketAssigned;
import io.monosense.synergyflow.itsm.events.TicketAssignmentAudit;
import io.monosense.synergyflow.itsm.events.TicketCreated;
import io.monosense.synergyflow.itsm.events.TicketReopenAudit;
import io.monosense.synergyflow.itsm.events.TicketResolutionAudit;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.exception.ConcurrentUpdateException;
import io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException;
import io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException;
import io.monosense.synergyflow.itsm.internal.repository.SlaTrackingRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketCommentRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import io.monosense.synergyflow.itsm.internal.service.SlaCalculator;
import io.monosense.synergyflow.itsm.internal.service.RoutingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing IT Service Management tickets with event-driven architecture.
 *
 * <p>This service handles the core business operations for tickets in the ITSM domain.
 * It demonstrates transactional event publishing by persisting domain events to the outbox
 * pattern within the same database transaction as aggregate state changes. This ensures
 * reliable event delivery and maintains consistency between command and query sides.</p>
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Create new tickets with automatic ticket number generation</li>
 *   <li>Assign tickets to agents or users</li>
 *   <li>Publish domain events for state changes</li>
 *   <li>Maintain transactional consistency between commands and events</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class TicketController {
 *     private final TicketService ticketService;
 *
 *     public UUID createNewTicket(String title, String priority, UUID requesterId) {
 *         return ticketService.createTicket(title, priority, requesterId);
 *     }
 *
 *     public void assignTicketToAgent(UUID ticketId, UUID agentId, UUID assignedBy) {
 *         ticketService.assignTicket(ticketId, agentId, assignedBy);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository repository;
    private final TicketCommentRepository commentRepository;
    private final SlaTrackingRepository slaTrackingRepository;
    private final SlaCalculator slaCalculator;
    private final EventPublisher eventPublisher;
    private final StateTransitionValidator stateTransitionValidator;
    private final RoutingEngine routingEngine;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new TicketService with the required dependencies.
     *
     * @param repository the JPA repository for ticket persistence
     * @param commentRepository the JPA repository for ticket comment persistence
     * @param slaTrackingRepository the JPA repository for SLA tracking persistence (Story 2.3)
     * @param slaCalculator the service for calculating SLA deadlines (Story 2.3)
     * @param eventPublisher the event publisher for domain events
     * @param stateTransitionValidator the validator for state transitions
     * @param meterRegistry the Micrometer meter registry for metrics tracking
     * @param objectMapper the JSON object mapper for event serialization
     * @throws IllegalArgumentException if any parameter is null
     * @since 2.2
     */
    public TicketService(TicketRepository repository,
                         TicketCommentRepository commentRepository,
                         SlaTrackingRepository slaTrackingRepository,
                         SlaCalculator slaCalculator,
                         EventPublisher eventPublisher,
                         StateTransitionValidator stateTransitionValidator,
                         MeterRegistry meterRegistry,
                         ObjectMapper objectMapper,
                         RoutingEngine routingEngine) {
        this.repository = repository;
        this.commentRepository = commentRepository;
        this.slaTrackingRepository = slaTrackingRepository;
        this.slaCalculator = slaCalculator;
        this.eventPublisher = eventPublisher;
        this.stateTransitionValidator = stateTransitionValidator;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.routingEngine = routingEngine;
    }

    /**
     * Creates a new ticket with the specified details and publishes a TicketCreated event.
     *
     * <p>This method creates a new ticket (Incident or ServiceRequest based on ticketType),
     * persists it to the database, and publishes a corresponding domain event. The operation
     * is performed within a single transaction to ensure consistency between the ticket
     * state and the published event.</p>
     *
     * <p>The method includes retry logic for handling concurrent updates via @Retryable annotation.
     * If OptimisticLockingFailureException occurs, the operation will be retried up to 4 times
     * with exponential backoff (50ms → 100ms → 200ms with jitter).</p>
     *
     * @param command the command object containing ticket creation parameters
     * @param currentUserId the unique identifier of the user creating the ticket
     * @return the persisted Ticket entity
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket createTicket(CreateTicketCommand command, UUID currentUserId) {
        // Track metrics
        meterRegistry.counter("ticket.create.attempts").increment();

        // Create ticket entity based on type
        Ticket ticket;
        if (command.ticketType() == TicketType.INCIDENT) {
            ticket = new Incident(
                    command.title(),
                    command.description(),
                    TicketStatus.NEW,
                    command.priority(),
                    command.category(),
                    command.requesterId(),
                    null,  // assigneeId - not assigned at creation
                    null   // severity - not set at creation
            );
        } else { // SERVICE_REQUEST
            ticket = new ServiceRequest(
                    command.title(),
                    command.description(),
                    TicketStatus.NEW,
                    command.priority(),
                    command.category(),
                    command.requesterId(),
                    null,  // assigneeId - not assigned at creation
                    null   // requestType - not set at creation
            );
        }

        // Persist ticket (ID generated automatically by @UuidGenerator)
        ticket = repository.save(ticket);

        // Create SLA tracking record for incidents with priority (Story 2.3 AC-5)
        if (ticket.getTicketType() == TicketType.INCIDENT && ticket.getPriority() != null) {
            java.time.Instant dueAt = slaCalculator.calculateDueAt(ticket.getPriority(), ticket.getCreatedAt());
            SlaTracking slaTracking = new SlaTracking(ticket.getId(), ticket.getPriority(), dueAt);
            slaTrackingRepository.save(slaTracking);
            log.debug("Created SLA tracking for incident {} with priority {} (due at {})",
                    ticket.getId(), ticket.getPriority(), dueAt);
        }

        // Apply routing rules (auto-assignment) before publishing TicketCreated
        try {
            routingEngine.applyRules(ticket);
        } catch (Exception ex) {
            log.warn("Routing engine failed for ticket {}: {}", ticket.getId(), ex.toString());
            meterRegistry.counter("routing.failures").increment();
        }

        // Prepare and publish TicketCreatedEvent
        TicketCreated event = new TicketCreated(
                ticket.getId(),
                "T-" + ticket.getId(), // Generate ticket number for event
                ticket.getTitle(),
                ticket.getStatus().name(),
                ticket.getPriority() != null ? ticket.getPriority().name() : null,
                ticket.getRequesterId(),
                ticket.getAssigneeId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketCreated",
                ticket.getVersion(),
                ticket.getCreatedAt(),
                payload
        );

        return ticket;
    }

    /**
     * Recovery method for createTicket when retry attempts are exhausted.
     *
     * <p>This method is invoked by Spring Retry when all retry attempts (4) have been
     * exhausted due to OptimisticLockingFailureException. It logs the error and throws
     * a user-friendly ConcurrentUpdateException.</p>
     *
     * @param ex the OptimisticLockingFailureException that caused the retry exhaustion
     * @param command the original command object
     * @param currentUserId the user ID from the original call
     * @throws ConcurrentUpdateException always, with a user-friendly message
     * @since 2.2
     */
    @Recover
    public Ticket recoverCreateTicket(OptimisticLockingFailureException ex,
                                      CreateTicketCommand command,
                                      UUID currentUserId) {
        log.error("Failed to create ticket after 4 attempts due to concurrent updates", ex);
        meterRegistry.counter("ticket.create.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket creation failed due to concurrent updates. Please try again.",
                ex
        );
    }

    /**
     * Assigns a ticket to the specified agent and publishes a TicketAssigned event.
     *
     * <p>This method validates the state transition from NEW to ASSIGNED, updates the ticket's
     * assignee and status, persists the change to the database, and publishes a corresponding
     * domain event. The operation is performed within a single transaction to ensure consistency
     * between the ticket state and the published event.</p>
     *
     * <p>Business validation is performed first to ensure exceptions are thrown without
     * Spring Retry wrapping. The actual state mutation is delegated to a retry-protected
     * internal method.</p>
     *
     * @param ticketId the unique identifier of the ticket to assign
     * @param assigneeId the unique identifier of the user to assign the ticket to
     * @param currentUserId the unique identifier of the user performing the assignment
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws MissingRequiredFieldException if assigneeId is null
     * @throws InvalidStateTransitionException if the current status is not NEW
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    public Ticket assignTicket(UUID ticketId, UUID assigneeId, UUID currentUserId) {
        // Validate input (prevents Spring Retry exception wrapping)
        validateAssignTicketInput(assigneeId);

        // Load ticket for initial validation
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate state transition
        stateTransitionValidator.validateTransition(ticket.getStatus(), TicketStatus.ASSIGNED);

        // Delegate to retry-protected internal method
        return assignTicketInternal(ticketId, assigneeId, currentUserId);
    }

    /**
     * Internal retry-protected method for assigning a ticket.
     * This method handles concurrent updates with retry logic.
     *
     * @param ticketId the unique identifier of the ticket to assign
     * @param assigneeId the unique identifier of the user to assign the ticket to
     * @param currentUserId the unique identifier of the user performing the assignment
     * @return the updated Ticket entity
     * @since 2.2
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    Ticket assignTicketInternal(UUID ticketId, UUID assigneeId, UUID currentUserId) {
        // Track metrics
        meterRegistry.counter("ticket.assign.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Re-validate state transition (handles race conditions during retries)
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.ASSIGNED);

        // Update ticket
        ticket.assignTo(assigneeId);
        ticket.updateStatus(TicketStatus.ASSIGNED);

        // Flush to ensure @Version increments before publishing the event
        ticket = repository.saveAndFlush(ticket);

        // Prepare and publish TicketAssigned event
        TicketAssigned event = new TicketAssigned(
                ticket.getId(),
                assigneeId,
                currentUserId,
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssigned",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        // Publish audit event for assignment operation
        TicketAssignmentAudit auditEvent = new TicketAssignmentAudit(
                ticket.getId(),
                assigneeId,
                null,  // previousAssigneeId is null for initial assignment
                currentUserId,
                "ASSIGN",
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode auditPayload = objectMapper.valueToTree(auditEvent);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssignmentAudit",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                auditPayload
        );

        return ticket;
    }

    /**
     * Recovery method for assignTicket when retry attempts are exhausted.
     *
     * @param ex the OptimisticLockingFailureException that caused the retry exhaustion
     * @param ticketId the ticket ID from the original call
     * @param assigneeId the assignee ID from the original call
     * @param currentUserId the user ID from the original call
     * @throws ConcurrentUpdateException always, with a user-friendly message
     * @since 2.2
     */
    @Recover
    public Ticket recoverAssignTicket(OptimisticLockingFailureException ex,
                                      UUID ticketId,
                                      UUID assigneeId,
                                      UUID currentUserId) {
        log.error("Failed to assign ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.assign.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Un-assigns a ticket, returning it to NEW status.
     *
     * <p>This method validates that the current user is either the current assignee or has
     * appropriate permissions, then clears the assignment and returns the ticket to NEW status.</p>
     *
     * @param ticketId the unique identifier of the ticket to unassign
     * @param currentUserId the unique identifier of the user performing the unassignment
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws UnauthorizedOperationException if current user is not the assignee or lacks permission
     * @throws InvalidStateTransitionException if the current status is not ASSIGNED
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        noRetryFor = {
            io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class,
            io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class,
            io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class,
            io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException.class
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket unassignTicket(UUID ticketId, UUID currentUserId) {
        meterRegistry.counter("ticket.unassign.attempts").increment();

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate authorization: current user must be assignee or have permission
        if (ticket.getAssigneeId() != null && !ticket.getAssigneeId().equals(currentUserId)) {
            throw new io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException(
                    "unassign", currentUserId);
        }

        // Validate state transition (ASSIGNED → NEW)
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.NEW);

        // Capture previous assignee before clearing
        UUID previousAssigneeId = ticket.getAssigneeId();

        // Update ticket
        ticket.clearAssignment();
        ticket.updateStatus(TicketStatus.NEW);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.NEW.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        // Publish audit event for unassignment operation
        TicketAssignmentAudit auditEvent = new TicketAssignmentAudit(
                ticket.getId(),
                null,  // currentAssigneeId is null after unassignment
                previousAssigneeId,
                currentUserId,
                "UNASSIGN",
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode auditPayload = objectMapper.valueToTree(auditEvent);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssignmentAudit",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                auditPayload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverUnassignTicket(OptimisticLockingFailureException ex,
                                        UUID ticketId,
                                        UUID currentUserId) {
        log.error("Failed to unassign ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.unassign.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Reassigns a ticket to a new assignee.
     *
     * <p>If the ticket is currently IN_PROGRESS, it will be paused (transitioned to ASSIGNED)
     * before reassigning. Validates that the current user is either the current assignee or
     * has appropriate permissions.</p>
     *
     * @param ticketId the unique identifier of the ticket to reassign
     * @param newAssigneeId the unique identifier of the new assignee
     * @param currentUserId the unique identifier of the user performing the reassignment
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws MissingRequiredFieldException if newAssigneeId is null
     * @throws UnauthorizedOperationException if current user is not the assignee or lacks permission
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        noRetryFor = {
            io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class,
            io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class,
            io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class,
            io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException.class
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket reassignTicket(UUID ticketId, UUID newAssigneeId, UUID currentUserId) {
        meterRegistry.counter("ticket.reassign.attempts").increment();

        // Validate required fields
        if (newAssigneeId == null) {
            throw new io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException(
                    "newAssigneeId", "reassign");
        }

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate authorization
        if (ticket.getAssigneeId() != null && !ticket.getAssigneeId().equals(currentUserId)) {
            throw new io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException(
                    "reassign", currentUserId);
        }

        // Capture previous assignee before reassignment
        UUID previousAssigneeId = ticket.getAssigneeId();

        // If currently IN_PROGRESS, pause work first
        if (ticket.getStatus() == TicketStatus.IN_PROGRESS) {
            ticket.updateStatus(TicketStatus.ASSIGNED);
        }

        // Update assignment
        ticket.assignTo(newAssigneeId);
        ticket.updateStatus(TicketStatus.ASSIGNED);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketAssigned event
        TicketAssigned event = new TicketAssigned(
                ticket.getId(),
                newAssigneeId,
                currentUserId,
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssigned",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        // Publish audit event for reassignment operation
        TicketAssignmentAudit auditEvent = new TicketAssignmentAudit(
                ticket.getId(),
                newAssigneeId,
                previousAssigneeId,
                currentUserId,
                "REASSIGN",
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode auditPayload = objectMapper.valueToTree(auditEvent);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketAssignmentAudit",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                auditPayload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverReassignTicket(OptimisticLockingFailureException ex,
                                        UUID ticketId,
                                        UUID newAssigneeId,
                                        UUID currentUserId) {
        log.error("Failed to reassign ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.reassign.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Starts work on an assigned ticket, transitioning it to IN_PROGRESS status.
     *
     * <p>Validates that the current user is the ticket assignee before allowing
     * the status transition.</p>
     *
     * <p>Business validation is performed first to ensure exceptions are thrown without
     * Spring Retry wrapping. The actual state mutation is delegated to a retry-protected
     * internal method.</p>
     *
     * @param ticketId the unique identifier of the ticket to start work on
     * @param currentUserId the unique identifier of the user starting work
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws UnauthorizedOperationException if current user is not the assignee
     * @throws InvalidStateTransitionException if the current status is not ASSIGNED
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    public Ticket startWork(UUID ticketId, UUID currentUserId) {
        // Load ticket for validation
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate authorization (prevents Spring Retry exception wrapping)
        validateStartWorkAuthorization(ticket, currentUserId);

        // Validate state transition
        stateTransitionValidator.validateTransition(ticket.getStatus(), TicketStatus.IN_PROGRESS);

        // Delegate to retry-protected internal method
        return startWorkInternal(ticketId, currentUserId);
    }

    /**
     * Internal retry-protected method for starting work on a ticket.
     * This method handles concurrent updates with retry logic.
     *
     * @param ticketId the unique identifier of the ticket to start work on
     * @param currentUserId the unique identifier of the user starting work
     * @return the updated Ticket entity
     * @since 2.2
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    Ticket startWorkInternal(UUID ticketId, UUID currentUserId) {
        meterRegistry.counter("ticket.startWork.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Re-validate authorization and state transition (handles race conditions during retries)
        validateStartWorkAuthorization(ticket, currentUserId);
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.IN_PROGRESS);

        // Update ticket
        ticket.updateStatus(TicketStatus.IN_PROGRESS);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.IN_PROGRESS.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverStartWork(OptimisticLockingFailureException ex,
                                   UUID ticketId,
                                   UUID currentUserId) {
        log.error("Failed to start work on ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.startWork.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Pauses work on a ticket, transitioning it back to ASSIGNED status.
     *
     * <p>Validates that the current user is the ticket assignee before allowing
     * the status transition.</p>
     *
     * @param ticketId the unique identifier of the ticket to pause work on
     * @param currentUserId the unique identifier of the user pausing work
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws UnauthorizedOperationException if current user is not the assignee
     * @throws InvalidStateTransitionException if the current status is not IN_PROGRESS
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        noRetryFor = {
            io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class,
            io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class,
            io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class,
            io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException.class
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket pauseWork(UUID ticketId, UUID currentUserId) {
        meterRegistry.counter("ticket.pauseWork.attempts").increment();

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate authorization: current user must be assignee
        if (!currentUserId.equals(ticket.getAssigneeId())) {
            throw new io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException(
                    "pauseWork", currentUserId);
        }

        // Validate state transition (IN_PROGRESS → ASSIGNED)
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.ASSIGNED);

        // Update ticket
        ticket.updateStatus(TicketStatus.ASSIGNED);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.ASSIGNED.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverPauseWork(OptimisticLockingFailureException ex,
                                   UUID ticketId,
                                   UUID currentUserId) {
        log.error("Failed to pause work on ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.pauseWork.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Resolves a ticket with resolution notes, transitioning it to RESOLVED status.
     *
     * <p>Validates that the current user is the ticket assignee and that resolution notes
     * are provided with at least 10 characters. For Incident tickets, sets resolvedAt timestamp.
     * For ServiceRequest tickets, sets fulfilledAt timestamp.</p>
     *
     * <p>Business validation is performed first to ensure exceptions are thrown without
     * Spring Retry wrapping. The actual state mutation is delegated to a retry-protected
     * internal method.</p>
     *
     * @param ticketId the unique identifier of the ticket to resolve
     * @param resolutionNotes detailed notes describing how the ticket was resolved
     * @param currentUserId the unique identifier of the user resolving the ticket
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws UnauthorizedOperationException if current user is not the assignee
     * @throws MissingRequiredFieldException if resolutionNotes is null or length < 10
     * @throws InvalidStateTransitionException if the current status is not IN_PROGRESS
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    public Ticket resolveTicket(UUID ticketId, String resolutionNotes, UUID currentUserId) {
        // Validate input only (prevents Spring Retry exception wrapping)
        validateResolveTicketInput(resolutionNotes);

        // Load ticket for state validation
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate state transition first (before authorization check)
        stateTransitionValidator.validateTransition(ticket.getStatus(), TicketStatus.RESOLVED);

        // Validate authorization
        validateResolveTicketAuthorization(ticket, currentUserId);

        // Delegate to retry-protected internal method
        return resolveTicketInternal(ticketId, resolutionNotes, currentUserId);
    }

    /**
     * Internal retry-protected method for resolving a ticket.
     * This method handles concurrent updates with retry logic.
     *
     * @param ticketId the unique identifier of the ticket to resolve
     * @param resolutionNotes detailed notes describing how the ticket was resolved
     * @param currentUserId the unique identifier of the user resolving the ticket
     * @return the updated Ticket entity
     * @since 2.2
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    Ticket resolveTicketInternal(UUID ticketId, String resolutionNotes, UUID currentUserId) {
        meterRegistry.counter("ticket.resolve.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Re-validate state transition and authorization (handles race conditions during retries)
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.RESOLVED);
        validateResolveTicketAuthorization(ticket, currentUserId);

        // Update ticket and set resolution timestamps based on type
        ticket.updateStatus(TicketStatus.RESOLVED);
        if (ticket instanceof Incident) {
            ((Incident) ticket).resolve(resolutionNotes);
        } else if (ticket instanceof ServiceRequest) {
            ((ServiceRequest) ticket).fulfill(currentUserId);
        }

        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.RESOLVED.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        // Publish audit event for resolution operation
        TicketResolutionAudit auditEvent = new TicketResolutionAudit(
                ticket.getId(),
                currentUserId,
                resolutionNotes,
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode auditPayload = objectMapper.valueToTree(auditEvent);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketResolutionAudit",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                auditPayload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverResolveTicket(OptimisticLockingFailureException ex,
                                       UUID ticketId,
                                       String resolutionNotes,
                                       UUID currentUserId) {
        log.error("Failed to resolve ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.resolve.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Closes a resolved ticket, transitioning it to CLOSED status.
     *
     * <p>Validates that the ticket is currently in RESOLVED status before allowing closure.</p>
     *
     * <p>Business validation is performed first to ensure exceptions are thrown without
     * Spring Retry wrapping. The actual state mutation is delegated to a retry-protected
     * internal method.</p>
     *
     * @param ticketId the unique identifier of the ticket to close
     * @param currentUserId the unique identifier of the user closing the ticket
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws InvalidStateTransitionException if the current status is not RESOLVED
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    public Ticket closeTicket(UUID ticketId, UUID currentUserId) {
        // Load ticket for validation
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate state transition (prevents Spring Retry exception wrapping)
        stateTransitionValidator.validateTransition(ticket.getStatus(), TicketStatus.CLOSED);

        // Delegate to retry-protected internal method
        return closeTicketInternal(ticketId, currentUserId);
    }

    /**
     * Internal retry-protected method for closing a ticket.
     * This method handles concurrent updates with retry logic.
     *
     * @param ticketId the unique identifier of the ticket to close
     * @param currentUserId the unique identifier of the user closing the ticket
     * @return the updated Ticket entity
     * @since 2.2
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    Ticket closeTicketInternal(UUID ticketId, UUID currentUserId) {
        meterRegistry.counter("ticket.close.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Re-validate state transition (handles race conditions during retries)
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.CLOSED);

        // Update ticket
        ticket.updateStatus(TicketStatus.CLOSED);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.CLOSED.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverCloseTicket(OptimisticLockingFailureException ex,
                                     UUID ticketId,
                                     UUID currentUserId) {
        log.error("Failed to close ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.close.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Reopens a resolved or closed ticket, returning it to NEW status.
     *
     * <p>Clears the assignment, resolution notes, and resolution timestamps. Increments
     * the reopen counter for tracking. Validates that a reopen reason is provided with
     * at least 10 characters. If reopening from CLOSED status, requires additional
     * authorization (manager approval - not implemented in this story).</p>
     *
     * <p>Business validation is performed first to ensure exceptions are thrown without
     * Spring Retry wrapping. The actual state mutation is delegated to a retry-protected
     * internal method.</p>
     *
     * @param ticketId the unique identifier of the ticket to reopen
     * @param reopenReason detailed reason for reopening the ticket
     * @param currentUserId the unique identifier of the user reopening the ticket
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws MissingRequiredFieldException if reopenReason is null or length < 10
     * @throws InvalidStateTransitionException if the current status is not RESOLVED or CLOSED
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    public Ticket reopenTicket(UUID ticketId, String reopenReason, UUID currentUserId) {
        // Validate input (prevents Spring Retry exception wrapping)
        validateReopenTicketInput(reopenReason);

        // Load ticket for validation
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate state transition
        stateTransitionValidator.validateTransition(ticket.getStatus(), TicketStatus.NEW);

        // Delegate to retry-protected internal method
        return reopenTicketInternal(ticketId, reopenReason, currentUserId);
    }

    /**
     * Internal retry-protected method for reopening a ticket.
     * This method handles concurrent updates with retry logic.
     *
     * @param ticketId the unique identifier of the ticket to reopen
     * @param reopenReason detailed reason for reopening the ticket
     * @param currentUserId the unique identifier of the user reopening the ticket
     * @return the updated Ticket entity
     * @since 2.2
     */
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    Ticket reopenTicketInternal(UUID ticketId, String reopenReason, UUID currentUserId) {
        meterRegistry.counter("ticket.reopen.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Re-validate input and state transition (handles race conditions during retries)
        validateReopenTicketInput(reopenReason);
        TicketStatus oldStatus = ticket.getStatus();
        stateTransitionValidator.validateTransition(oldStatus, TicketStatus.NEW);

        // TODO: Manager approval for reopen-from-CLOSED deferred to Story 2.5 (Authorization & Permissions)
        // When CLOSED → NEW, should verify currentUserId has manager role via AuthorizationService
        // For now, both RESOLVED → NEW and CLOSED → NEW are allowed without approval

        // Clear assignment and resolution fields
        ticket.clearAssignment();
        if (ticket instanceof Incident) {
            ((Incident) ticket).clearResolution();
        } else if (ticket instanceof ServiceRequest) {
            ((ServiceRequest) ticket).clearFulfillment();
        }

        // Increment reopen count
        ticket.incrementReopenCount();

        // Update status
        ticket.updateStatus(TicketStatus.NEW);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        oldStatus.name(),
                        TicketStatus.NEW.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        // Publish audit event for reopen operation
        TicketReopenAudit auditEvent = new TicketReopenAudit(
                ticket.getId(),
                currentUserId,
                reopenReason,
                ticket.getReopenCount(),
                ticket.getUpdatedAt(),
                ticket.getVersion()
        );

        ObjectNode auditPayload = objectMapper.valueToTree(auditEvent);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketReopenAudit",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                auditPayload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverReopenTicket(OptimisticLockingFailureException ex,
                                      UUID ticketId,
                                      String reopenReason,
                                      UUID currentUserId) {
        log.error("Failed to reopen ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.reopen.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Updates the priority of a ticket.
     *
     * <p>Validates that the ticket is not RESOLVED or CLOSED, as priority is locked
     * after resolution.</p>
     *
     * <p>This method handles concurrent updates with retry logic via @Retryable.
     * If OptimisticLockingFailureException occurs, the operation will be retried up to 4 times
     * with exponential backoff (50ms → 100ms → 200ms with jitter).</p>
     *
     * @param ticketId the unique identifier of the ticket
     * @param newPriority the new priority level
     * @param currentUserId the unique identifier of the user updating the priority
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws InvalidStateTransitionException if the ticket is RESOLVED or CLOSED
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        include = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket updatePriority(UUID ticketId, Priority newPriority, UUID currentUserId) {
        meterRegistry.counter("ticket.updatePriority.attempts").increment();

        // Reload ticket for fresh state (required for retry safety)
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Validate priority lock (must be done on each retry attempt to handle race conditions)
        validatePriorityUpdate(ticket);

        // Update priority
        TicketStatus currentStatus = ticket.getStatus();
        ticket.updatePriority(newPriority);
        ticket = repository.saveAndFlush(ticket);

        // Recalculate SLA for incidents (Story 2.3 AC-6, AC-7)
        // Skip if ticket is resolved or closed (AC-7: SLA frozen for historical accuracy)
        if (ticket.getTicketType() == TicketType.INCIDENT &&
            currentStatus != TicketStatus.RESOLVED &&
            currentStatus != TicketStatus.CLOSED) {

            // Fetch existing SLA record or create new one if ticket was created without priority
            var existingSla = slaTrackingRepository.findByTicketId(ticket.getId());

            // Recalculate dueAt from ORIGINAL ticket.createdAt (not current time) to preserve fairness
            java.time.Instant newDueAt = slaCalculator.calculateDueAt(newPriority, ticket.getCreatedAt());

            if (existingSla.isPresent()) {
                // Update existing SLA record
                SlaTracking slaTracking = existingSla.get();
                slaTracking.updateSla(newPriority, newDueAt);
                slaTrackingRepository.save(slaTracking);
                log.debug("Recalculated SLA for incident {} with new priority {} (due at {})",
                        ticket.getId(), newPriority, newDueAt);
            } else {
                // Create new SLA record (handles case where ticket was created without priority)
                SlaTracking slaTracking = new SlaTracking(ticket.getId(), newPriority, newDueAt);
                slaTrackingRepository.save(slaTracking);
                log.debug("Created SLA tracking for incident {} after priority added (due at {})",
                        ticket.getId(), newDueAt);
            }
        }

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        currentStatus.name(),
                        currentStatus.name(),  // Status didn't change, but priority did
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverUpdatePriority(Exception ex,
                                        UUID ticketId,
                                        Priority newPriority,
                                        UUID currentUserId) {
        // Handle retryable exceptions that exhausted retries
        if (ex instanceof OptimisticLockingFailureException) {
            log.error("Failed to update priority for ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
            meterRegistry.counter("ticket.updatePriority.retries_exhausted").increment();
            throw new ConcurrentUpdateException(
                    "Ticket is being updated by another user. Please try again.",
                    ex
            );
        }

        // For non-retryable exceptions (like InvalidStateTransitionException), just rethrow them
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new RuntimeException(ex);
    }

    /**
     * Updates the category of a ticket.
     *
     * @param ticketId the unique identifier of the ticket
     * @param newCategory the new category classification
     * @param currentUserId the unique identifier of the user updating the category
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket updateCategory(UUID ticketId, String newCategory, UUID currentUserId) {
        meterRegistry.counter("ticket.updateCategory.attempts").increment();

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus currentStatus = ticket.getStatus();
        ticket.updateCategory(newCategory);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        currentStatus.name(),
                        currentStatus.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverUpdateCategory(OptimisticLockingFailureException ex,
                                        UUID ticketId,
                                        String newCategory,
                                        UUID currentUserId) {
        log.error("Failed to update category for ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.updateCategory.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Updates the title of a ticket.
     *
     * @param ticketId the unique identifier of the ticket
     * @param newTitle the new title (must not be blank)
     * @param currentUserId the unique identifier of the user updating the title
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws IllegalArgumentException if newTitle is blank
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        noRetryFor = {
            io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class,
            io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class,
            io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class,
            io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException.class
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket updateTitle(UUID ticketId, String newTitle, UUID currentUserId) {
        meterRegistry.counter("ticket.updateTitle.attempts").increment();

        if (newTitle == null || newTitle.isBlank()) {
            throw new IllegalArgumentException("Title cannot be blank");
        }

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus currentStatus = ticket.getStatus();
        ticket.updateTitle(newTitle);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        currentStatus.name(),
                        currentStatus.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverUpdateTitle(OptimisticLockingFailureException ex,
                                     UUID ticketId,
                                     String newTitle,
                                     UUID currentUserId) {
        log.error("Failed to update title for ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.updateTitle.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Updates the description of a ticket.
     *
     * @param ticketId the unique identifier of the ticket
     * @param newDescription the new description
     * @param currentUserId the unique identifier of the user updating the description
     * @return the updated Ticket entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @throws ConcurrentUpdateException if retry attempts are exhausted due to concurrent updates
     * @since 2.2
     */
    @Transactional
    @Retryable(
        value = {OptimisticLockingFailureException.class},
        noRetryFor = {
            io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class,
            io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class,
            io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class,
            io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException.class
        },
        maxAttempts = 4,
        backoff = @Backoff(delay = 50, multiplier = 2, maxDelay = 200, random = true)
    )
    public Ticket updateDescription(UUID ticketId, String newDescription, UUID currentUserId) {
        meterRegistry.counter("ticket.updateDescription.attempts").increment();

        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        TicketStatus currentStatus = ticket.getStatus();
        ticket.updateDescription(newDescription);
        ticket = repository.saveAndFlush(ticket);

        // Publish TicketStateChanged event
        io.monosense.synergyflow.itsm.events.TicketStateChanged event =
                new io.monosense.synergyflow.itsm.events.TicketStateChanged(
                        ticket.getId(),
                        currentStatus.name(),
                        currentStatus.name(),
                        ticket.getUpdatedAt(),
                        ticket.getVersion()
                );

        ObjectNode payload = objectMapper.valueToTree(event);
        eventPublisher.publish(
                ticket.getId(),
                "TICKET",
                "TicketStateChanged",
                ticket.getVersion(),
                ticket.getUpdatedAt(),
                payload
        );

        return ticket;
    }

    @Recover
    public Ticket recoverUpdateDescription(OptimisticLockingFailureException ex,
                                           UUID ticketId,
                                           String newDescription,
                                           UUID currentUserId) {
        log.error("Failed to update description for ticket {} after 4 attempts due to concurrent updates", ticketId, ex);
        meterRegistry.counter("ticket.updateDescription.retries_exhausted").increment();
        throw new ConcurrentUpdateException(
                "Ticket is being updated by another user. Please try again.",
                ex
        );
    }

    /**
     * Adds a comment to an existing ticket.
     *
     * <p>This method creates a new comment on the specified ticket and persists it to the database.
     * The comment can be either public (visible to requesters) or internal (agent-only).
     * The method verifies that the ticket exists before creating the comment.</p>
     *
     * <p>Note: Comment events are NOT published in this story (deferred to future implementation).</p>
     *
     * @param ticketId the unique identifier of the ticket to comment on
     * @param command the command object containing comment details (text and visibility)
     * @param currentUserId the unique identifier of the user adding the comment
     * @return the persisted TicketComment entity
     * @throws TicketNotFoundException if the ticket with the specified ID does not exist
     * @since 2.2
     */
    @Transactional
    public TicketComment addComment(UUID ticketId, AddCommentCommand command, UUID currentUserId) {
        // Verify ticket exists (throws TicketNotFoundException if not found)
        repository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // Create new comment entity
        TicketComment comment = new TicketComment(
                ticketId,
                currentUserId,
                command.commentText(),
                command.isInternal()
        );

        // Persist and return comment
        return commentRepository.save(comment);
    }

    // ===================================
    // Validation Methods (called BEFORE @Retryable methods to prevent Spring Retry exception wrapping)
    // ===================================

    /**
     * Validates input parameters for assignTicket operation.
     *
     * @param assigneeId the assignee ID to validate
     * @throws io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException if assigneeId is null
     * @since 2.2
     */
    private void validateAssignTicketInput(UUID assigneeId) {
        if (assigneeId == null) {
            throw new io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException(
                    "assigneeId", "assign");
        }
    }

    /**
     * Validates authorization for startWork operation.
     *
     * @param ticket the ticket to validate
     * @param currentUserId the current user ID
     * @throws io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException if current user is not the assignee
     * @since 2.2
     */
    private void validateStartWorkAuthorization(Ticket ticket, UUID currentUserId) {
        if (!currentUserId.equals(ticket.getAssigneeId())) {
            throw new io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException(
                    "startWork", currentUserId);
        }
    }

    /**
     * Validates input parameters for resolveTicket operation.
     *
     * @param resolutionNotes the resolution notes to validate
     * @throws io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException if resolutionNotes is invalid
     * @since 2.2
     */
    private void validateResolveTicketInput(String resolutionNotes) {
        if (resolutionNotes == null || resolutionNotes.length() < 10) {
            throw new io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException(
                    "resolutionNotes", "resolve");
        }
    }

    /**
     * Validates authorization for resolveTicket operation.
     *
     * @param ticket the ticket to validate
     * @param currentUserId the current user ID
     * @throws io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException if current user is not the assignee
     * @since 2.2
     */
    private void validateResolveTicketAuthorization(Ticket ticket, UUID currentUserId) {
        if (!currentUserId.equals(ticket.getAssigneeId())) {
            throw new io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException(
                    "resolve", currentUserId);
        }
    }

    /**
     * Validates input parameters for reopenTicket operation.
     *
     * @param reopenReason the reopen reason to validate
     * @throws io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException if reopenReason is invalid
     * @since 2.2
     */
    private void validateReopenTicketInput(String reopenReason) {
        if (reopenReason == null || reopenReason.length() < 10) {
            throw new io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException(
                    "reopenReason", "reopen");
        }
    }

    /**
     * Validates that priority update is allowed on the ticket.
     *
     * @param ticket the ticket to validate
     * @throws io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException if ticket is RESOLVED or CLOSED
     * @since 2.2
     */
    private void validatePriorityUpdate(Ticket ticket) {
        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            throw new io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException(
                    "Priority cannot be changed on " + ticket.getStatus() + " tickets", ticket.getStatus());
        }
    }
}
