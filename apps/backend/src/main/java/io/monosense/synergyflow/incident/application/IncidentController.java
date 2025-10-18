package io.monosense.synergyflow.incident.application;

import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.domain.Incident;
import io.monosense.synergyflow.incident.domain.IncidentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for incident management.
 *
 * <p>Publishes IncidentCreatedEvent via service layer,
 * which triggers transactional outbox persistence.
 */
@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody CreateIncidentRequest request) {
        Incident incident = new Incident();
        incident.setTitle(request.title());
        incident.setDescription(request.description());
        incident.setPriority(request.priority());
        incident.setReportedBy(request.reportedBy());

        Incident created = incidentService.createIncident(incident);

        IncidentResponse response = new IncidentResponse(
            created.getIncidentId(),
            created.getTitle(),
            created.getDescription(),
            created.getPriority(),
            created.getStatus(),
            created.getReportedBy(),
            created.getCreatedAt()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    public record CreateIncidentRequest(
        @NotBlank String title,
        String description,
        @NotNull IncidentCreatedEvent.Priority priority,
        @NotBlank String reportedBy
    ) {}

    public record IncidentResponse(
        String incidentId,
        String title,
        String description,
        IncidentCreatedEvent.Priority priority,
        Incident.IncidentStatus status,
        String reportedBy,
        java.time.Instant createdAt
    ) {}
}
