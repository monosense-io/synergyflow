package io.monosense.synergyflow.eventing.internal.worker;

import io.monosense.synergyflow.eventing.readmodel.IssueCardEntity;
import io.monosense.synergyflow.eventing.readmodel.IssueCardRepository;
import io.monosense.synergyflow.eventing.readmodel.QueueRowEntity;
import io.monosense.synergyflow.eventing.readmodel.QueueRowRepository;
import io.monosense.synergyflow.eventing.readmodel.mapping.IssueCardMapper;
import io.monosense.synergyflow.eventing.readmodel.mapping.QueueRowMapper;
import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.worker.model.IssueStateChangedMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IssueReadModelHandlerTest {

    private IssueCardRepository issueCardRepository;
    private QueueRowRepository queueRowRepository;
    private IssueReadModelHandler handler;

    @BeforeEach
    void setUp() {
        issueCardRepository = mock(IssueCardRepository.class);
        queueRowRepository = mock(QueueRowRepository.class);
        IssueCardMapper issueCardMapper = Mappers.getMapper(IssueCardMapper.class);
        QueueRowMapper queueRowMapper = Mappers.getMapper(QueueRowMapper.class);
        handler = new IssueReadModelHandler(issueCardRepository, queueRowRepository, issueCardMapper, queueRowMapper);
    }

    @Test
    void createsIssueCardAndQueueRow() {
        UUID issueId = UUID.randomUUID();
        IssueCreatedMessage message = new IssueCreatedMessage(
                issueId,
                "ISS-12",
                "Investigate incident",
                "BUG",
                "TODO",
                "MEDIUM",
                UUID.randomUUID(),
                null,
                null,
                Instant.parse("2025-10-07T09:00:00Z"),
                Instant.parse("2025-10-07T09:00:00Z")
        );

        when(issueCardRepository.upsertWithVersionGuard(any(IssueCardEntity.class))).thenReturn(true);
        when(queueRowRepository.upsertWithVersionGuard(any(QueueRowEntity.class))).thenReturn(true);

        List<ReadModelUpdateResult> results = handler.handleIssueCreated(message, 1L);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(ReadModelUpdateResult::updated);
        verify(issueCardRepository).upsertWithVersionGuard(any(IssueCardEntity.class));
        verify(queueRowRepository).upsertWithVersionGuard(any(QueueRowEntity.class));
    }

    @Test
    void skipsWhenVersionStale() {
        UUID issueId = UUID.randomUUID();
        when(issueCardRepository.upsertWithVersionGuard(any(IssueCardEntity.class))).thenReturn(false);
        when(queueRowRepository.upsertWithVersionGuard(any(QueueRowEntity.class))).thenReturn(false);

        IssueCreatedMessage message = new IssueCreatedMessage(
                issueId,
                "ISS-12",
                "Updated",
                "BUG",
                "TODO",
                "MEDIUM",
                UUID.randomUUID(),
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        List<ReadModelUpdateResult> results = handler.handleIssueCreated(message, 3L);

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(result -> !result.updated());
        verify(issueCardRepository).upsertWithVersionGuard(any(IssueCardEntity.class));
        verify(queueRowRepository).upsertWithVersionGuard(any(QueueRowEntity.class));
    }

    @Test
    void updatesStateOnIssueStateChanged() {
        UUID issueId = UUID.randomUUID();
        when(issueCardRepository.updateStateWithVersionGuard(eq(issueId), anyString(), any(Instant.class), eq(2L)))
                .thenReturn(true);
        when(queueRowRepository.updateStateWithVersionGuard(eq(issueId), anyString(), eq(2L))).thenReturn(true);

        IssueStateChangedMessage message = new IssueStateChangedMessage(
                issueId,
                "TODO",
                "IN_PROGRESS",
                UUID.randomUUID(),
                null,
                null,
                2L,
                Instant.parse("2025-10-07T12:00:00Z")
        );

        List<ReadModelUpdateResult> results = handler.handleIssueStateChanged(message);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).updated()).isTrue();
        assertThat(results.get(1).updated()).isTrue();
        verify(issueCardRepository).updateStateWithVersionGuard(eq(issueId), eq("IN_PROGRESS"),
                eq(Instant.parse("2025-10-07T12:00:00Z")), eq(2L));
        verify(queueRowRepository).updateStateWithVersionGuard(eq(issueId), eq("IN_PROGRESS"), eq(2L));
    }
}
