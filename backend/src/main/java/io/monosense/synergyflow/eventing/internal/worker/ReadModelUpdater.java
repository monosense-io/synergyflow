package io.monosense.synergyflow.eventing.internal.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.monosense.synergyflow.eventing.worker.model.IssueCreatedMessage;
import io.monosense.synergyflow.eventing.worker.model.IssueStateChangedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketAssignedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Coordinates read model projections for each outbox event type.
 * <p>
 * This service is responsible for updating various read models based on domain events
 * that are processed from the transactional outbox. It acts as a central dispatcher
 * that routes events to appropriate handlers based on event type, ensuring that
 * materialized views stay synchronized with the write model.
 * </p>
 * <p>
 * The service handles the following event types:
 * <ul>
 *     <li>TicketCreated - Updates ticket read models</li>
 *     <li>TicketAssigned - Updates ticket assignment information</li>
 *     <li>IssueCreated - Updates issue read models</li>
 *     <li>IssueStateChanged - Updates issue state transitions</li>
 * </ul>
 * </p>
 * <p>
 * Usage example:
 * <pre>
 * readModelUpdater.update(outboxMessage);
 * </pre>
 * </p>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@Slf4j
@Service
@Profile("worker")
@RequiredArgsConstructor
public class ReadModelUpdater {

    private final ObjectMapper objectMapper;
    private final TicketReadModelHandler ticketReadModelHandler;
    private final IssueReadModelHandler issueReadModelHandler;
    private final MeterRegistry meterRegistry;

    /**
     * Updates read models based on the provided outbox event.
     * <p>
     * This method deserializes the event payload and routes it to the appropriate
     * handler based on the event type. It records metrics for successful updates
     * and handles errors gracefully.
     * </p>
     *
     * @param event the outbox message containing the event to be processed
     */
    public void update(OutboxMessage event) {
        String eventType = event.eventType();
        try {
            JsonNode payload = event.payload();
            List<ReadModelUpdateResult> results = switch (eventType) {
                case "TicketCreated" -> handleTicketCreated(payload, event.version());
                case "TicketAssigned" -> handleTicketAssigned(payload, event.version());
                case "IssueCreated" -> handleIssueCreated(payload, event.version());
                case "IssueStateChanged" -> handleIssueStateChanged(payload);
                default -> {
                    log.debug("No read model handler for event_type={}, skipping", eventType);
                    yield Collections.emptyList();
                }
            };

            for (ReadModelUpdateResult result : results) {
                if (result.updated()) {
                    incrementSuccess(eventType, result.modelName());
                }
            }
        } catch (Exception ex) {
            recordError(eventType, ex);
            log.error("Failed to update read model for event_type={}: {}", eventType, ex.getMessage(), ex);
        }
    }

    private List<ReadModelUpdateResult> handleTicketCreated(JsonNode payload, long version) throws com.fasterxml.jackson.core.JsonProcessingException {
        TicketCreatedMessage message = objectMapper.treeToValue(payload, TicketCreatedMessage.class);
        return List.of(ticketReadModelHandler.handleTicketCreated(message, version));
    }

    private List<ReadModelUpdateResult> handleTicketAssigned(JsonNode payload, long version) throws com.fasterxml.jackson.core.JsonProcessingException {
        TicketAssignedMessage message = objectMapper.treeToValue(payload, TicketAssignedMessage.class);
        return List.of(ticketReadModelHandler.handleTicketAssigned(message));
    }

    private List<ReadModelUpdateResult> handleIssueCreated(JsonNode payload, long version) throws com.fasterxml.jackson.core.JsonProcessingException {
        IssueCreatedMessage message = objectMapper.treeToValue(payload, IssueCreatedMessage.class);
        return issueReadModelHandler.handleIssueCreated(message, version);
    }

    private List<ReadModelUpdateResult> handleIssueStateChanged(JsonNode payload) throws com.fasterxml.jackson.core.JsonProcessingException {
        IssueStateChangedMessage message = objectMapper.treeToValue(payload, IssueStateChangedMessage.class);
        return issueReadModelHandler.handleIssueStateChanged(message);
    }

    private void incrementSuccess(String eventType, String modelName) {
        Counter.builder("outbox_read_model_updates_total")
                .tag("event_type", eventType)
                .tag("model", modelName)
                .register(meterRegistry)
                .increment();
    }

    private void recordError(String eventType, Throwable ex) {
        Counter.builder("outbox_read_model_update_errors_total")
                .tag("event_type", eventType)
                .tag("error", ex.getClass().getSimpleName())
                .register(meterRegistry)
                .increment();
    }
}
