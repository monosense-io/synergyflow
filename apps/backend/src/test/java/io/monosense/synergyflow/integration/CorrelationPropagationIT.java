package io.monosense.synergyflow.integration;

import io.monosense.synergyflow.incident.domain.IncidentService;
import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CorrelationPropagationIT {

    @Autowired
    IncidentService incidentService;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void correlationId_isPresentOnPublishedEvent() {
        // Use service via controller/service layer (domain service exists)
        var incident = new io.monosense.synergyflow.incident.domain.Incident();
        incident.setTitle("API 500s");
        incident.setDescription("Failing");
        incident.setPriority(IncidentCreatedEvent.Priority.P2_HIGH);
        incident.setReportedBy("user-123");
        incidentService.createIncident(incident);

        String payload = jdbc.queryForObject(
            "select serialized_event from event_publication order by publication_date desc limit 1",
            String.class
        );
        assertThat(payload).isNotNull();
        assertThat(payload).contains("correlationId");
        assertThat(payload).contains("causationId");
    }
}
