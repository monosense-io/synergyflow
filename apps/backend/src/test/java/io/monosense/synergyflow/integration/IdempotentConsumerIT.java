package io.monosense.synergyflow.integration;

import io.monosense.synergyflow.common.PostgreSQLTestContainer;
import io.monosense.synergyflow.incident.api.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.domain.Incident;
import io.monosense.synergyflow.incident.domain.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for AC#3: Idempotent event consumers.
 *
 * <p>Verifies:
 * <ol>
 *   <li>First consumption creates projection (audit log)</li>
 *   <li>Second consumption skips due to processed_events check</li>
 *   <li>No duplicate rows in projection table</li>
 *   <li>Integration test using event replay</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
class IdempotentConsumerIT extends PostgreSQLTestContainer {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldProcessEventOnlyOnceEvenWithDuplicateDelivery() throws InterruptedException {
        // Given: Create an incident (publishes event once)
        Incident incident = new Incident();
        incident.setTitle("Idempotency Test");
        incident.setDescription("Testing duplicate event handling");
        incident.setPriority(IncidentCreatedEvent.Priority.P4_LOW);
        incident.setReportedBy("user-999");

        Incident created = incidentService.createIncident(incident);

        // Wait for first event processing
        await().atMost(20, TimeUnit.SECONDS).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_log WHERE entity_id = ?",
                Integer.class,
                created.getIncidentId()
            );
            assertThat(count).isEqualTo(1);
        });

        // When: Simulate duplicate event delivery by publishing again
        IncidentCreatedEvent duplicateEvent = new IncidentCreatedEvent(
            created.getIncidentId(),
            created.getTitle(),
            created.getDescription(),
            created.getPriority(),
            created.getReportedBy(),
            "correlation-duplicate",
            "causation-duplicate",
            Instant.now()
        );

        eventPublisher.publishEvent(duplicateEvent);

        // Give time for async processing
        Thread.sleep(2000);

        // Then: Still only ONE audit log entry (no duplicate)
        Integer auditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_log WHERE entity_id = ?",
            Integer.class,
            created.getIncidentId()
        );
        assertThat(auditCount).isEqualTo(1);

        // Verify processed_events table has entries
        Integer processedCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM processed_events WHERE event_type LIKE '%IncidentCreatedEvent%'",
            Integer.class
        );
        assertThat(processedCount).isGreaterThan(0);
    }
}
