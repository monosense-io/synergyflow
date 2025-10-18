package io.monosense.synergyflow.incident.domain;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.monosense.synergyflow.common.events.CorrelationContext;
import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident service implementation.
 *
 * <p>Publishes IncidentCreatedEvent via ApplicationEventPublisher.
 * Spring Modulith stores the event in event_publication table atomically
 * within the same transaction as the incident entity.
 */
@Service
public class IncidentServiceImpl extends ServiceImpl<IncidentMapper, Incident> implements IncidentService {

    private final ApplicationEventPublisher eventPublisher;

    public IncidentServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Incident createIncident(Incident incident) {
        // Generate UUID for public incident ID
        incident.setIncidentId(UUID.randomUUID().toString());
        incident.setStatus(Incident.IncidentStatus.NEW);
        incident.setCreatedAt(Instant.now());
        incident.setUpdatedAt(Instant.now());
        incident.setDeleted(0);

        // Save incident (MyBatis-Plus)
        save(incident);

        // Publish domain event
        // Spring Modulith stores this in event_publication table within the same transaction
        IncidentCreatedEvent event = new IncidentCreatedEvent(
            incident.getIncidentId(),
            incident.getTitle(),
            incident.getDescription(),
            incident.getPriority(),
            incident.getReportedBy(),
            CorrelationContext.getOrCreateCorrelationId(),
            CorrelationContext.getOrCreateCorrelationId(), // Causation = Correlation for user-initiated
            incident.getCreatedAt()
        );

        eventPublisher.publishEvent(event);

        return incident;
    }
}
