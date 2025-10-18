package io.monosense.synergyflow.incident;

import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.domain.Incident;
import io.monosense.synergyflow.incident.domain.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class IncidentEventOutboxIT {

    @Autowired
    IncidentService incidentService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void creatingIncident_persistsEventPublicationRow() {
        Incident incident = new Incident();
        incident.setTitle("Production API down");
        incident.setDescription("500 errors");
        incident.setPriority(IncidentCreatedEvent.Priority.P2_HIGH);
        incident.setReportedBy("user-123");

        incidentService.createIncident(incident);

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from event_publication where event_type = ?",
                Integer.class,
                "io.monosense.synergyflow.incident.api.IncidentCreatedEvent"
        );
        assertThat(count).isNotNull();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}
