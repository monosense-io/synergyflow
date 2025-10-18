package io.monosense.synergyflow.time;

import io.monosense.synergyflow.SynergyFlowApplication;
import io.monosense.synergyflow.time.api.TimeEntryCreatedEvent;
import io.monosense.synergyflow.time.application.consumers.IncidentWorklogConsumer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SynergyFlowApplication.class)
@Disabled("Pending full schema alignment and containerized DB setup; enable when CI provides PostgreSQL")
class TimeEntryMirroringIT {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    IncidentWorklogConsumer incidentWorklogConsumer;

    @Test
    void shouldMirrorToIncidentWorklogAndUpdateAggregate() {
        // Arrange: create an incident with public ID
        String publicId = "incident-123";
        jdbcTemplate.update("INSERT INTO incidents (incident_id, title, priority, status, reported_by) VALUES (?,?,?,?,?)",
                publicId, "Test Incident", "P3_MEDIUM", "NEW", "tester");

        var event = new TimeEntryCreatedEvent(
                "time-001",
                "user-1",
                Duration.ofMinutes(30),
                "Investigated issue",
                Instant.now(),
                List.of(new TimeEntryCreatedEvent.TargetEntity(TimeEntryCreatedEvent.TargetEntity.EntityType.INCIDENT, publicId, "Test Incident")),
                "corr-1",
                "corr-1",
                Instant.now()
        );

        // Act
        incidentWorklogConsumer.handleTimeEntryCreated(event);

        // Assert worklog row and aggregate
        Integer worklogCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incident_worklogs WHERE incident_id = ? AND time_entry_id = ?",
                Integer.class, publicId, "time-001");
        assertThat(worklogCount).isNotNull();
        assertThat(worklogCount).isGreaterThanOrEqualTo(1);

        Long aggregate = jdbcTemplate.queryForObject(
                "SELECT total_time_minutes FROM incidents WHERE incident_id = ?",
                Long.class, publicId);
        assertThat(aggregate).isNotNull();
        assertThat(aggregate).isGreaterThanOrEqualTo(30);
    }
}

