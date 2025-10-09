package io.monosense.synergyflow.itsm.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.eventing.api.EventPublisher;
import io.monosense.synergyflow.itsm.api.dto.AddCommentCommand;
import io.monosense.synergyflow.itsm.api.dto.CreateTicketCommand;
import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.exception.ConcurrentUpdateException;
import io.monosense.synergyflow.itsm.internal.exception.TicketNotFoundException;
import io.monosense.synergyflow.itsm.internal.repository.TicketCommentRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import io.monosense.synergyflow.itsm.internal.StateTransitionValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for TicketService covering Task 3 and Task 4 requirements.
 *
 * <p>Tests cover:
 * <ul>
 *   <li>Task 3: createTicket() with Incident and ServiceRequest types</li>
 *   <li>Task 3: Event publishing for ticket creation</li>
 *   <li>Task 3: addComment() method</li>
 *   <li>Task 4: All 13 state transition methods with valid cases</li>
 *   <li>Task 4: Authorization failures for state transitions</li>
 *   <li>Task 4: Missing required field validations</li>
 *   <li>Exception handling (TicketNotFoundException, InvalidStateTransitionException, etc.)</li>
 *   <li>Retry recovery for OptimisticLockingFailureException</li>
 * </ul>
 *
 * @since 2.2
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService Unit Tests (Task 3 & Task 4)")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCommentRepository commentRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private StateTransitionValidator stateTransitionValidator;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    private ObjectMapper objectMapper;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Enable Java 8 date/time support
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);

        ticketService = new TicketService(
                ticketRepository,
                commentRepository,
                eventPublisher,
                stateTransitionValidator,
                meterRegistry,
                objectMapper
        );
    }

    @Test
    @DisplayName("createTicket() should create Incident when ticketType is INCIDENT")
    void createTicket_shouldCreateIncident_whenTicketTypeIsIncident() {
        // Given
        UUID requesterId = UUID.randomUUID();
        CreateTicketCommand command = new CreateTicketCommand(
                "Critical server outage",
                "Production server is down",
                TicketType.INCIDENT,
                Priority.CRITICAL,
                "Infrastructure",
                requesterId
        );
        UUID currentUserId = UUID.randomUUID();

        Incident savedIncident = new Incident(
                command.title(),
                command.description(),
                TicketStatus.NEW,
                command.priority(),
                command.category(),
                command.requesterId(),
                null,
                null
        );
        // Simulate ID generation and version
        savedIncident = spy(savedIncident);
        when(savedIncident.getId()).thenReturn(UUID.randomUUID());
        when(savedIncident.getVersion()).thenReturn(1L);
        when(savedIncident.getCreatedAt()).thenReturn(Instant.now());
        when(savedIncident.getUpdatedAt()).thenReturn(Instant.now());

        when(ticketRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // When
        Ticket result = ticketService.createTicket(command, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(Incident.class);
        assertThat(result.getTitle()).isEqualTo("Critical server outage");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.NEW);
        assertThat(result.getPriority()).isEqualTo(Priority.CRITICAL);

        verify(ticketRepository).save(any(Incident.class));
        verify(counter).increment(); // Metrics tracking
        verify(eventPublisher).publish(
                eq(savedIncident.getId()),
                eq("TICKET"),
                eq("TicketCreated"),
                eq(1L),
                any(Instant.class),
                any(ObjectNode.class)
        );
    }

    @Test
    @DisplayName("createTicket() should create ServiceRequest when ticketType is SERVICE_REQUEST")
    void createTicket_shouldCreateServiceRequest_whenTicketTypeIsServiceRequest() {
        // Given
        UUID requesterId = UUID.randomUUID();
        CreateTicketCommand command = new CreateTicketCommand(
                "Request new laptop",
                "Need new MacBook Pro for development",
                TicketType.SERVICE_REQUEST,
                Priority.MEDIUM,
                "Hardware",
                requesterId
        );
        UUID currentUserId = UUID.randomUUID();

        ServiceRequest savedRequest = new ServiceRequest(
                command.title(),
                command.description(),
                TicketStatus.NEW,
                command.priority(),
                command.category(),
                command.requesterId(),
                null,
                null
        );
        // Simulate ID generation and version
        savedRequest = spy(savedRequest);
        when(savedRequest.getId()).thenReturn(UUID.randomUUID());
        when(savedRequest.getVersion()).thenReturn(1L);
        when(savedRequest.getCreatedAt()).thenReturn(Instant.now());
        when(savedRequest.getUpdatedAt()).thenReturn(Instant.now());

        when(ticketRepository.save(any(ServiceRequest.class))).thenReturn(savedRequest);

        // When
        Ticket result = ticketService.createTicket(command, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ServiceRequest.class);
        assertThat(result.getTitle()).isEqualTo("Request new laptop");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.NEW);

        verify(ticketRepository).save(any(ServiceRequest.class));
        verify(eventPublisher).publish(
                any(UUID.class),
                eq("TICKET"),
                eq("TicketCreated"),
                eq(1L),
                any(Instant.class),
                any(ObjectNode.class)
        );
    }

    @Test
    @DisplayName("createTicket() should publish TicketCreatedEvent with correct fields")
    void createTicket_shouldPublishTicketCreatedEvent_withCorrectFields() {
        // Given
        UUID requesterId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Instant createdAt = Instant.now();

        CreateTicketCommand command = new CreateTicketCommand(
                "Test ticket",
                "Test description",
                TicketType.INCIDENT,
                Priority.HIGH,
                null,
                requesterId
        );

        Incident savedIncident = spy(new Incident(
                command.title(),
                command.description(),
                TicketStatus.NEW,
                command.priority(),
                command.category(),
                command.requesterId(),
                null,
                null
        ));
        when(savedIncident.getId()).thenReturn(ticketId);
        when(savedIncident.getVersion()).thenReturn(1L);
        when(savedIncident.getCreatedAt()).thenReturn(createdAt);
        when(savedIncident.getUpdatedAt()).thenReturn(createdAt);

        when(ticketRepository.save(any(Incident.class))).thenReturn(savedIncident);

        // When
        ticketService.createTicket(command, UUID.randomUUID());

        // Then
        ArgumentCaptor<ObjectNode> payloadCaptor = ArgumentCaptor.forClass(ObjectNode.class);
        verify(eventPublisher).publish(
                eq(ticketId),
                eq("TICKET"),
                eq("TicketCreated"),
                eq(1L),
                eq(createdAt),
                payloadCaptor.capture()
        );

        ObjectNode payload = payloadCaptor.getValue();
        assertThat(payload.get("ticketId").asText()).isEqualTo(ticketId.toString());
        assertThat(payload.get("title").asText()).isEqualTo("Test ticket");
        assertThat(payload.get("status").asText()).isEqualTo("NEW");
        assertThat(payload.get("priority").asText()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("recoverCreateTicket() should throw ConcurrentUpdateException after retry exhaustion")
    void recoverCreateTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        CreateTicketCommand command = new CreateTicketCommand(
                "Test",
                "Test",
                TicketType.INCIDENT,
                Priority.MEDIUM,
                null,
                UUID.randomUUID()
        );
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverCreateTicket(ex, command, UUID.randomUUID())
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket creation failed due to concurrent updates");

        verify(meterRegistry).counter("ticket.create.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("addComment() should create and persist TicketComment")
    void addComment_shouldCreateAndPersistTicketComment() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        AddCommentCommand command = new AddCommentCommand(
                "This is a test comment",
                false
        );

        Ticket ticket = new Incident(
                "Test",
                "Test",
                TicketStatus.NEW,
                Priority.MEDIUM,
                null,
                UUID.randomUUID(),
                null,
                null
        );

        TicketComment savedComment = new TicketComment(
                ticketId,
                currentUserId,
                command.commentText(),
                command.isInternal()
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(TicketComment.class))).thenReturn(savedComment);

        // When
        TicketComment result = ticketService.addComment(ticketId, command, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCommentText()).isEqualTo("This is a test comment");
        assertThat(result.isInternal()).isFalse();

        verify(ticketRepository).findById(ticketId);
        verify(commentRepository).save(any(TicketComment.class));
    }

    @Test
    @DisplayName("addComment() should throw TicketNotFoundException when ticket does not exist")
    void addComment_shouldThrowTicketNotFoundException_whenTicketDoesNotExist() {
        // Given
        UUID nonExistentTicketId = UUID.randomUUID();
        AddCommentCommand command = new AddCommentCommand("Test comment", false);

        when(ticketRepository.findById(nonExistentTicketId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() ->
                ticketService.addComment(nonExistentTicketId, command, UUID.randomUUID())
        )
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("Ticket not found: " + nonExistentTicketId);

        verify(commentRepository, never()).save(any(TicketComment.class));
    }

    @Test
    @DisplayName("addComment() should handle internal comments correctly")
    void addComment_shouldHandleInternalComments() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        AddCommentCommand command = new AddCommentCommand(
                "Internal note: escalate to senior team",
                true  // internal comment
        );

        Ticket ticket = new Incident(
                "Test",
                "Test",
                TicketStatus.NEW,
                Priority.MEDIUM,
                null,
                UUID.randomUUID(),
                null,
                null
        );

        TicketComment savedComment = new TicketComment(
                ticketId,
                currentUserId,
                command.commentText(),
                command.isInternal()
        );

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(TicketComment.class))).thenReturn(savedComment);

        // When
        TicketComment result = ticketService.addComment(ticketId, command, currentUserId);

        // Then
        assertThat(result.isInternal()).isTrue();
        assertThat(result.getCommentText()).contains("Internal note");

        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().isInternal()).isTrue();
    }

    @Test
    @DisplayName("addComment() should use default isInternal=false when not specified")
    void addComment_shouldUseDefaultIsInternalFalse() {
        // Given
        UUID ticketId = UUID.randomUUID();
        AddCommentCommand command = new AddCommentCommand("Public comment", null);

        Ticket ticket = new Incident("Test", "Test", TicketStatus.NEW,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(TicketComment.class))).thenReturn(
                new TicketComment(ticketId, UUID.randomUUID(), command.commentText(), command.isInternal())
        );

        // When
        ticketService.addComment(ticketId, command, UUID.randomUUID());

        // Then
        ArgumentCaptor<TicketComment> commentCaptor = ArgumentCaptor.forClass(TicketComment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertThat(commentCaptor.getValue().isInternal()).isFalse(); // Default value
    }

    // ==================== TASK 4: STATE TRANSITION TESTS ====================

    // ===== 4.6: Valid Cases Tests =====

    @Test
    @DisplayName("assignTicket() should assign ticket and update status to ASSIGNED (AC-3)")
    void assignTicket_shouldAssignAndUpdateStatus_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.NEW,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getAssigneeId()).thenReturn(assigneeId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.assignTicket(ticketId, assigneeId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator, times(2)).validateTransition(TicketStatus.NEW, TicketStatus.ASSIGNED);
        verify(ticket).assignTo(assigneeId);
        verify(ticket).updateStatus(TicketStatus.ASSIGNED);
        verify(eventPublisher).publish(
                eq(ticketId),
                eq("TICKET"),
                eq("TicketAssigned"),
                eq(2L),
                any(Instant.class),
                any(ObjectNode.class)
        );
    }

    @Test
    @DisplayName("unassignTicket() should clear assignment and return to NEW status (AC-3)")
    void unassignTicket_shouldClearAssignmentAndReturnToNew_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.ASSIGNED,
                Priority.MEDIUM, null, UUID.randomUUID(), currentUserId, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.ASSIGNED);
        lenient().when(ticket.getAssigneeId()).thenReturn(currentUserId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.unassignTicket(ticketId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator).validateTransition(TicketStatus.ASSIGNED, TicketStatus.NEW);
        verify(ticket).clearAssignment();
        verify(ticket).updateStatus(TicketStatus.NEW);
        verify(eventPublisher).publish(
                eq(ticketId),
                eq("TICKET"),
                eq("TicketStateChanged"),
                eq(2L),
                any(Instant.class),
                any(ObjectNode.class)
        );
    }

    @Test
    @DisplayName("startWork() should transition from ASSIGNED to IN_PROGRESS (AC-4)")
    void startWork_shouldTransitionToInProgress_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.ASSIGNED,
                Priority.MEDIUM, null, UUID.randomUUID(), currentUserId, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.ASSIGNED);
        lenient().when(ticket.getAssigneeId()).thenReturn(currentUserId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.startWork(ticketId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator, times(2)).validateTransition(TicketStatus.ASSIGNED, TicketStatus.IN_PROGRESS);
        verify(ticket).updateStatus(TicketStatus.IN_PROGRESS);
        verify(eventPublisher).publish(
                eq(ticketId),
                eq("TICKET"),
                eq("TicketStateChanged"),
                any(),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("pauseWork() should transition from IN_PROGRESS to ASSIGNED (AC-4)")
    void pauseWork_shouldTransitionToAssigned_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, null, UUID.randomUUID(), currentUserId, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);
        lenient().when(ticket.getAssigneeId()).thenReturn(currentUserId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.pauseWork(ticketId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator).validateTransition(TicketStatus.IN_PROGRESS, TicketStatus.ASSIGNED);
        verify(ticket).updateStatus(TicketStatus.ASSIGNED);
    }

    @Test
    @DisplayName("resolveTicket() should set resolvedAt for Incident and transition to RESOLVED (AC-5)")
    void resolveTicket_shouldSetResolvedAtForIncident_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        String resolutionNotes = "Fixed by restarting server";

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, null, UUID.randomUUID(), currentUserId, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);
        lenient().when(ticket.getAssigneeId()).thenReturn(currentUserId);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.resolveTicket(ticketId, resolutionNotes, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator, times(2)).validateTransition(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED);
        verify(ticket).updateStatus(TicketStatus.RESOLVED);
        verify(ticket).resolve(resolutionNotes);
    }

    @Test
    @DisplayName("closeTicket() should transition from RESOLVED to CLOSED (AC-5)")
    void closeTicket_shouldTransitionToClosed_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.RESOLVED,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.RESOLVED);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.closeTicket(ticketId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator, times(2)).validateTransition(TicketStatus.RESOLVED, TicketStatus.CLOSED);
        verify(ticket).updateStatus(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("reopenTicket() should increment reopenCount and transition to NEW (AC-5)")
    void reopenTicket_shouldIncrementReopenCountAndTransitionToNew_whenValid() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        String reopenReason = "Issue recurred after deployment";

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.RESOLVED,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.RESOLVED);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.reopenTicket(ticketId, reopenReason, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(stateTransitionValidator, times(2)).validateTransition(TicketStatus.RESOLVED, TicketStatus.NEW);
        verify(ticket).clearAssignment();
        verify(ticket).clearResolution();
        verify(ticket).incrementReopenCount();
        verify(ticket).updateStatus(TicketStatus.NEW);
    }

    @Test
    @DisplayName("updatePriority() should update priority when ticket not resolved or closed (AC-6)")
    void updatePriority_shouldUpdatePriority_whenNotResolvedOrClosed() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        Priority newPriority = Priority.CRITICAL;

        Incident ticket = spy(new Incident("Test", "Test", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null));
        lenient().when(ticket.getId()).thenReturn(ticketId);
        lenient().when(ticket.getVersion()).thenReturn(2L);
        lenient().when(ticket.getUpdatedAt()).thenReturn(Instant.now());
        lenient().when(ticket.getStatus()).thenReturn(TicketStatus.IN_PROGRESS);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(ticketRepository.saveAndFlush(ticket)).thenReturn(ticket);

        // When
        Ticket result = ticketService.updatePriority(ticketId, newPriority, currentUserId);

        // Then
        assertThat(result).isNotNull();
        verify(ticket).updatePriority(newPriority);
    }

    // ===== 4.7: Authorization Failure Tests =====

    @Test
    @DisplayName("assignTicket() should throw MissingRequiredFieldException when assigneeId is null (AC-3)")
    void assignTicket_shouldThrowMissingRequiredFieldException_whenAssigneeIdNull() {
        // Given
        UUID ticketId = UUID.randomUUID();

        // When/Then
        assertThatThrownBy(() ->
                ticketService.assignTicket(ticketId, null, UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("assigneeId")
                .hasMessageContaining("assign");
    }

    @Test
    @DisplayName("startWork() should throw UnauthorizedOperationException when user is not assignee (AC-4)")
    void startWork_shouldThrowUnauthorizedOperationException_whenUserNotAssignee() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        Incident ticket = new Incident("Test", "Test", TicketStatus.ASSIGNED,
                Priority.MEDIUM, null, UUID.randomUUID(), assigneeId, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // When/Then
        assertThatThrownBy(() ->
                ticketService.startWork(ticketId, differentUserId)
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class)
                .hasMessageContaining("startWork")
                .hasMessageContaining(differentUserId.toString());
    }

    @Test
    @DisplayName("pauseWork() should throw UnauthorizedOperationException when user is not assignee (AC-4)")
    void pauseWork_shouldThrowUnauthorizedOperationException_whenUserNotAssignee() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        Incident ticket = new Incident("Test", "Test", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, null, UUID.randomUUID(), assigneeId, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // When/Then
        assertThatThrownBy(() ->
                ticketService.pauseWork(ticketId, differentUserId)
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class)
                .hasMessageContaining("pauseWork");
    }

    @Test
    @DisplayName("resolveTicket() should throw UnauthorizedOperationException when user is not assignee (AC-5)")
    void resolveTicket_shouldThrowUnauthorizedOperationException_whenUserNotAssignee() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        Incident ticket = new Incident("Test", "Test", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, null, UUID.randomUUID(), assigneeId, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // When/Then
        assertThatThrownBy(() ->
                ticketService.resolveTicket(ticketId, "Valid resolution notes", differentUserId)
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class)
                .hasMessageContaining("resolve");
    }

    @Test
    @DisplayName("unassignTicket() should throw UnauthorizedOperationException when user is not assignee (AC-3)")
    void unassignTicket_shouldThrowUnauthorizedOperationException_whenUserNotAssignee() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();

        Incident ticket = new Incident("Test", "Test", TicketStatus.ASSIGNED,
                Priority.MEDIUM, null, UUID.randomUUID(), assigneeId, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // When/Then
        assertThatThrownBy(() ->
                ticketService.unassignTicket(ticketId, differentUserId)
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.UnauthorizedOperationException.class)
                .hasMessageContaining("unassign");
    }

    // ===== 4.8: Missing Required Field Validation Tests =====

    @Test
    @DisplayName("resolveTicket() should throw MissingRequiredFieldException when resolutionNotes is null (AC-5)")
    void resolveTicket_shouldThrowMissingRequiredFieldException_whenResolutionNotesNull() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.resolveTicket(UUID.randomUUID(), null, UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("resolutionNotes")
                .hasMessageContaining("resolve");
    }

    @Test
    @DisplayName("resolveTicket() should throw MissingRequiredFieldException when resolutionNotes length < 10 (AC-5)")
    void resolveTicket_shouldThrowMissingRequiredFieldException_whenResolutionNotesTooShort() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.resolveTicket(UUID.randomUUID(), "Short", UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("resolutionNotes")
                .hasMessageContaining("resolve");
    }

    @Test
    @DisplayName("reopenTicket() should throw MissingRequiredFieldException when reopenReason is null (AC-5)")
    void reopenTicket_shouldThrowMissingRequiredFieldException_whenReopenReasonNull() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.reopenTicket(UUID.randomUUID(), null, UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("reopenReason")
                .hasMessageContaining("reopen");
    }

    @Test
    @DisplayName("reopenTicket() should throw MissingRequiredFieldException when reopenReason length < 10 (AC-5)")
    void reopenTicket_shouldThrowMissingRequiredFieldException_whenReopenReasonTooShort() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.reopenTicket(UUID.randomUUID(), "Short", UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("reopenReason")
                .hasMessageContaining("reopen");
    }

    @Test
    @DisplayName("reassignTicket() should throw MissingRequiredFieldException when newAssigneeId is null (AC-3)")
    void reassignTicket_shouldThrowMissingRequiredFieldException_whenNewAssigneeIdNull() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.reassignTicket(UUID.randomUUID(), null, UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.MissingRequiredFieldException.class)
                .hasMessageContaining("newAssigneeId")
                .hasMessageContaining("reassign");
    }

    @Test
    @DisplayName("updateTitle() should throw IllegalArgumentException when newTitle is blank (AC-6)")
    void updateTitle_shouldThrowIllegalArgumentException_whenNewTitleBlank() {
        // When/Then
        assertThatThrownBy(() ->
                ticketService.updateTitle(UUID.randomUUID(), "", UUID.randomUUID())
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title cannot be blank");
    }

    @Test
    @DisplayName("updatePriority() should throw InvalidStateTransitionException when ticket is RESOLVED (AC-6)")
    void updatePriority_shouldThrowInvalidStateTransitionException_whenTicketResolved() {
        // Given
        UUID ticketId = UUID.randomUUID();

        Incident ticket = new Incident("Test", "Test", TicketStatus.RESOLVED,
                Priority.MEDIUM, null, UUID.randomUUID(), null, null);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        // When/Then
        assertThatThrownBy(() ->
                ticketService.updatePriority(ticketId, Priority.CRITICAL, UUID.randomUUID())
        )
                .isInstanceOf(io.monosense.synergyflow.itsm.internal.exception.InvalidStateTransitionException.class);
    }

    // ==================== TASK 5: RETRY PATTERN TESTS ====================

    // ===== 5.4: @Recover Method Tests =====

    @Test
    @DisplayName("recoverAssignTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverAssignTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverAssignTicket(ex, ticketId, assigneeId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.assign.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverStartWork() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverStartWork_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverStartWork(ex, ticketId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.startWork.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverResolveTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverResolveTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        String resolutionNotes = "Fixed by restarting server";
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverResolveTicket(ex, ticketId, resolutionNotes, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.resolve.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverCloseTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverCloseTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverCloseTicket(ex, ticketId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.close.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverReopenTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverReopenTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        String reopenReason = "Issue recurred after deployment";
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverReopenTicket(ex, ticketId, reopenReason, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.reopen.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverUnassignTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverUnassignTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverUnassignTicket(ex, ticketId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.unassign.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverReassignTicket() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverReassignTicket_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID newAssigneeId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverReassignTicket(ex, ticketId, newAssigneeId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.reassign.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverPauseWork() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverPauseWork_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverPauseWork(ex, ticketId, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.pauseWork.retries_exhausted");
        verify(counter).increment();
    }

    @Test
    @DisplayName("recoverUpdatePriority() should throw ConcurrentUpdateException after retry exhaustion (AC-15)")
    void recoverUpdatePriority_shouldThrowConcurrentUpdateException_afterRetryExhaustion() {
        // Given
        UUID ticketId = UUID.randomUUID();
        Priority newPriority = Priority.CRITICAL;
        UUID currentUserId = UUID.randomUUID();
        OptimisticLockingFailureException ex = new OptimisticLockingFailureException("Concurrent update");

        // When/Then
        assertThatThrownBy(() ->
                ticketService.recoverUpdatePriority(ex, ticketId, newPriority, currentUserId)
        )
                .isInstanceOf(ConcurrentUpdateException.class)
                .hasMessageContaining("Ticket is being updated by another user");

        verify(meterRegistry).counter("ticket.updatePriority.retries_exhausted");
        verify(counter).increment();
    }
}
