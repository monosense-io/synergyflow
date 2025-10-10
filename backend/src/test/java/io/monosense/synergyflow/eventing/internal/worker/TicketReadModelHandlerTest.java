package io.monosense.synergyflow.eventing.internal.worker;

import io.monosense.synergyflow.eventing.readmodel.mapping.TicketCardMapper;
import io.monosense.synergyflow.eventing.worker.model.TicketAssignedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import io.monosense.synergyflow.eventing.readmodel.TicketCardEntity;
import io.monosense.synergyflow.eventing.readmodel.TicketCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketReadModelHandlerTest {

    private TicketCardRepository repository;
    private TicketReadModelHandler handler;

    @BeforeEach
    void setUp() {
        repository = mock(TicketCardRepository.class);
        TicketCardMapper mapper = Mappers.getMapper(TicketCardMapper.class);
        handler = new TicketReadModelHandler(repository, mapper, new UserLookup(mock(org.springframework.jdbc.core.JdbcTemplate.class)));
    }

    @Test
    void insertsNewTicketCardOnTicketCreated() {
        UUID ticketId = UUID.randomUUID();
        TicketCreatedMessage message = new TicketCreatedMessage(
                ticketId,
                "T-100",
                "New ticket",
                "OPEN",
                "HIGH",
                UUID.randomUUID(),
                null,
                null,
               null,
                Instant.parse("2025-10-07T10:15:30Z"),
                Instant.parse("2025-10-07T10:15:30Z")
        );

        when(repository.upsertWithVersionGuard(any(TicketCardEntity.class))).thenReturn(true);

        ReadModelUpdateResult result = handler.handleTicketCreated(message, 1L);

        assertThat(result.updated()).isTrue();
        ArgumentCaptor<TicketCardEntity> entityCaptor = ArgumentCaptor.forClass(TicketCardEntity.class);
        verify(repository).upsertWithVersionGuard(entityCaptor.capture());
        TicketCardEntity saved = entityCaptor.getValue();
        assertThat(saved.getTicketId()).isEqualTo(ticketId);
        assertThat(saved.getVersion()).isEqualTo(1L);
        assertThat(saved.getRequesterName()).isEqualTo("Requester " + message.requesterId());
        assertThat(saved.getRequesterEmail()).contains(message.requesterId().toString());
    }

    @Test
    void skipsUpdateWhenVersionIsStale() {
        UUID ticketId = UUID.randomUUID();

        when(repository.upsertWithVersionGuard(any(TicketCardEntity.class))).thenReturn(false);

        TicketCreatedMessage message = new TicketCreatedMessage(
                ticketId,
                "T-100",
                "Update",
                "OPEN",
                "HIGH",
                UUID.randomUUID(),
                null,
                "Requester",
                "requester@example.com",
                Instant.now(),
                Instant.now()
        );

        ReadModelUpdateResult result = handler.handleTicketCreated(message, 2L);

        assertThat(result.updated()).isFalse();
        verify(repository).upsertWithVersionGuard(any(TicketCardEntity.class));
    }

    @Test
    void updatesAssigneeWhenVersionAdvances() {
        UUID ticketId = UUID.randomUUID();
        when(repository.updateAssignmentWithVersionGuard(eq(ticketId), anyString(), anyString(), any(Instant.class), eq(2L)))
                .thenReturn(true);

        TicketAssignedMessage message = new TicketAssignedMessage(
                ticketId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Agent Smith",
                "agent@example.com",
                Instant.parse("2025-10-07T11:00:00Z"),
                2L
        );

        ReadModelUpdateResult result = handler.handleTicketAssigned(message);

        assertThat(result.updated()).isTrue();
        verify(repository).updateAssignmentWithVersionGuard(eq(ticketId), eq("Agent Smith"), eq("agent@example.com"),
                eq(Instant.parse("2025-10-07T11:00:00Z")), eq(2L));
    }

    @Test
    void skipsAssignmentWhenRepositoryReportsStaleVersion() {
        UUID ticketId = UUID.randomUUID();
        TicketAssignedMessage message = new TicketAssignedMessage(
                ticketId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Agent Smith",
                "agent@example.com",
                Instant.parse("2025-10-07T11:00:00Z"),
                1L
        );

        when(repository.updateAssignmentWithVersionGuard(eq(ticketId), anyString(), anyString(), any(Instant.class), eq(1L)))
                .thenReturn(false);
        when(repository.findByTicketId(ticketId)).thenReturn(Optional.of(TicketCardEntity.builder()
                .id(UUID.randomUUID())
                .ticketId(ticketId)
                .version(3L)
                .build()));

        ReadModelUpdateResult result = handler.handleTicketAssigned(message);

        assertThat(result.updated()).isFalse();
        verify(repository).updateAssignmentWithVersionGuard(eq(ticketId), eq("Agent Smith"), eq("agent@example.com"),
                eq(Instant.parse("2025-10-07T11:00:00Z")), eq(1L));
    }
}
