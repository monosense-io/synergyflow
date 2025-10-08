package io.monosense.synergyflow.eventing.internal.worker;

import io.monosense.synergyflow.eventing.readmodel.mapping.TicketCardMapper;
import io.monosense.synergyflow.eventing.worker.model.TicketAssignedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import io.monosense.synergyflow.eventing.readmodel.TicketCardEntity;
import io.monosense.synergyflow.eventing.readmodel.TicketCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Applies ticket-related event projections into denormalized read models.
 *
 * @since 1.6
 */
@Slf4j
@Service
@Profile("worker")
@RequiredArgsConstructor
public class TicketReadModelHandler {

    private static final String MODEL_NAME = "ticket_card";

    private final TicketCardRepository ticketCardRepository;
    private final TicketCardMapper ticketCardMapper;

    public ReadModelUpdateResult handleTicketCreated(TicketCreatedMessage message, long version) {
        TicketCardEntity mapped = ticketCardMapper.toTicketCard(message, version);
        applyRequesterDefaults(mapped, message);

        boolean updated = ticketCardRepository.upsertWithVersionGuard(mapped);
        return updated ? ReadModelUpdateResult.applied(MODEL_NAME) : ReadModelUpdateResult.skipped(MODEL_NAME);
    }

    public ReadModelUpdateResult handleTicketAssigned(TicketAssignedMessage message) {
        UUID ticketId = message.ticketId();
        Optional<TicketCardEntity> current = Optional.empty();
        long eventVersion;
        if (message.version() != null) {
            eventVersion = message.version();
        } else {
            current = ticketCardRepository.findByTicketId(ticketId);
            eventVersion = current.map(TicketCardEntity::getVersion).orElse(0L);
        }

        String assigneeName = (message.assigneeName() == null || message.assigneeName().isBlank())
                ? "Unassigned"
                : message.assigneeName();
        String assigneeEmail = (message.assigneeEmail() == null || message.assigneeEmail().isBlank())
                ? "unassigned@example.com"
                : message.assigneeEmail();
        Instant updatedAt = message.assignedAt() != null ? message.assignedAt() : Instant.now();

        boolean updated = ticketCardRepository.updateAssignmentWithVersionGuard(
                ticketId,
                assigneeName,
                assigneeEmail,
                updatedAt,
                eventVersion
        );

        if (!updated) {
            if (current.isEmpty()) {
                current = ticketCardRepository.findByTicketId(ticketId);
            }
            if (current.isEmpty()) {
                log.warn("Skipping TicketAssigned read model update: ticket_card row missing for ticket_id={}", ticketId);
            }
            return ReadModelUpdateResult.skipped(MODEL_NAME);
        }

        return ReadModelUpdateResult.applied(MODEL_NAME);
    }

    private void applyRequesterDefaults(TicketCardEntity entity, TicketCreatedMessage message) {
        if (entity.getRequesterName() == null || entity.getRequesterName().isBlank()) {
            entity.setRequesterName("Requester " + message.requesterId());
        }
        if (entity.getRequesterEmail() == null || entity.getRequesterEmail().isBlank()) {
            entity.setRequesterEmail(message.requesterId() != null ? message.requesterId() + "@example.com" : "unknown@example.com");
        }
        if (entity.getTicketNumber() == null || entity.getTicketNumber().isBlank()) {
            entity.setTicketNumber(message.ticketId().toString());
        }
    }
}
