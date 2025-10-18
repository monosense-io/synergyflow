package io.monosense.synergyflow.audit.internal;

import io.monosense.synergyflow.common.idempotency.ProcessedEventRepository;
import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Idempotent consumer example: writes a single audit_log row per incident.
 */
@Component
public class IncidentAuditConsumer {
    private final JdbcTemplate jdbc;
    private final ProcessedEventRepository processed;

    IncidentAuditConsumer(JdbcTemplate jdbc, ProcessedEventRepository processed) {
        this.jdbc = jdbc;
        this.processed = processed;
    }

    @ApplicationModuleListener
    @org.springframework.context.event.EventListener
    void on(IncidentCreatedEvent event) {
        String key = ProcessedEventRepository.generateKey(event.incidentId(), getClass());
        if (processed.isProcessed(key)) {
            return; // idempotent skip
        }

        // Create audit projection
        jdbc.update(
            "INSERT INTO audit_log(entity_type, entity_id, action, details, created_at) VALUES (?,?,?,?, current_timestamp)",
            "Incident", event.incidentId(), "CREATED", event.title()
        );

        processed.markAsProcessed(key, event.getClass().getName(), event.correlationId(), event.causationId());
    }
}
