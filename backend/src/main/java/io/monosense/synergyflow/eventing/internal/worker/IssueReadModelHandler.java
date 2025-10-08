package io.monosense.synergyflow.eventing.internal.worker;

import io.monosense.synergyflow.eventing.readmodel.mapping.IssueCardMapper;
import io.monosense.synergyflow.eventing.readmodel.mapping.QueueRowMapper;
import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.worker.model.IssueStateChangedMessage;
import io.monosense.synergyflow.eventing.readmodel.IssueCardEntity;
import io.monosense.synergyflow.eventing.readmodel.IssueCardRepository;
import io.monosense.synergyflow.eventing.readmodel.QueueRowEntity;
import io.monosense.synergyflow.eventing.readmodel.QueueRowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Updates issue-centric read models in response to project management events.
 *
 * @since 1.6
 */
@Slf4j
@Service
@Profile("worker")
@RequiredArgsConstructor
public class IssueReadModelHandler {

    private static final String ISSUE_CARD_MODEL = "issue_card";
    private static final String QUEUE_ROW_MODEL = "queue_row";

    private final IssueCardRepository issueCardRepository;
    private final QueueRowRepository queueRowRepository;
    private final IssueCardMapper issueCardMapper;
    private final QueueRowMapper queueRowMapper;

    public List<ReadModelUpdateResult> handleIssueCreated(IssueCreatedMessage message, long version) {
        List<ReadModelUpdateResult> results = new ArrayList<>(2);
        results.add(upsertIssueCard(message, version));
        results.add(upsertQueueRow(message, version));
        return results;
    }

    public List<ReadModelUpdateResult> handleIssueStateChanged(IssueStateChangedMessage message) {
        List<ReadModelUpdateResult> results = new ArrayList<>(2);
        results.add(updateIssueCardState(message));
        results.add(updateQueueRowState(message));
        return results;
    }

    private ReadModelUpdateResult upsertIssueCard(IssueCreatedMessage message, long version) {
        IssueCardEntity mapped = issueCardMapper.toIssueCard(message, version);
        boolean updated = issueCardRepository.upsertWithVersionGuard(mapped);
        return updated ? ReadModelUpdateResult.applied(ISSUE_CARD_MODEL) : ReadModelUpdateResult.skipped(ISSUE_CARD_MODEL);
    }

    private ReadModelUpdateResult upsertQueueRow(IssueCreatedMessage message, long version) {
        QueueRowEntity mapped = queueRowMapper.toQueueRow(message, version);
        boolean updated = queueRowRepository.upsertWithVersionGuard(mapped);
        return updated ? ReadModelUpdateResult.applied(QUEUE_ROW_MODEL) : ReadModelUpdateResult.skipped(QUEUE_ROW_MODEL);
    }

    private ReadModelUpdateResult updateIssueCardState(IssueStateChangedMessage message) {
        Optional<IssueCardEntity> existing = Optional.empty();
        long eventVersion;
        if (message.version() != null) {
            eventVersion = message.version();
        } else {
            existing = issueCardRepository.findByIssueId(message.issueId());
            eventVersion = existing.map(IssueCardEntity::getVersion).orElse(0L);
        }

        Instant updatedAt = message.changedAt() != null ? message.changedAt() : Instant.now();
        boolean updated = issueCardRepository.updateStateWithVersionGuard(
                message.issueId(),
                message.toState(),
                updatedAt,
                eventVersion
        );

        if (!updated) {
            if (existing.isEmpty()) {
                existing = issueCardRepository.findByIssueId(message.issueId());
            }
            if (existing.isEmpty()) {
                log.warn("Skipping IssueStateChanged issue_card update: missing issue_id={}", message.issueId());
            }
            return ReadModelUpdateResult.skipped(ISSUE_CARD_MODEL);
        }

        return ReadModelUpdateResult.applied(ISSUE_CARD_MODEL);
    }

    private ReadModelUpdateResult updateQueueRowState(IssueStateChangedMessage message) {
        Optional<QueueRowEntity> existing = Optional.empty();
        long eventVersion;
        if (message.version() != null) {
            eventVersion = message.version();
        } else {
            existing = queueRowRepository.findByTicketId(message.issueId());
            eventVersion = existing.map(QueueRowEntity::getVersion).orElse(0L);
        }

        boolean updated = queueRowRepository.updateStateWithVersionGuard(
                message.issueId(),
                message.toState(),
                eventVersion
        );

        if (!updated) {
            if (existing.isEmpty()) {
                existing = queueRowRepository.findByTicketId(message.issueId());
            }
            if (existing.isEmpty()) {
                log.debug("Queue row missing for issue_id={}, skip status sync", message.issueId());
            }
            return ReadModelUpdateResult.skipped(QUEUE_ROW_MODEL);
        }

        return ReadModelUpdateResult.applied(QUEUE_ROW_MODEL);
    }
}
