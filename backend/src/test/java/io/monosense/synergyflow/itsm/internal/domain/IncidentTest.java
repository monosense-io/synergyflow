package io.monosense.synergyflow.itsm.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Incident} entity.
 *
 * <p>Tests incident-specific behavior including constructor initialization,
 * ticketType discriminator, and domain methods for resolution and severity management.</p>
 *
 * @since 2.1
 */
@DisplayName("Incident Entity Unit Tests")
class IncidentTest {

    @Test
    @DisplayName("Constructor should set ticketType to INCIDENT")
    void testConstructorSetsTicketType() {
        // Given: Incident parameters
        Incident incident = createTestIncident();

        // Then: ticketType should be set to INCIDENT
        assertThat(incident.getTicketType()).isEqualTo(TicketType.INCIDENT);
    }

    @Test
    @DisplayName("Constructor should initialize all fields including severity")
    void testConstructorInitialization() {
        // Given: Incident parameters with all fields
        String title = "Server outage in production";
        String description = "Database server not responding";
        TicketStatus status = TicketStatus.NEW;
        Priority priority = Priority.CRITICAL;
        String category = "Infrastructure";
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        Severity severity = Severity.CRITICAL;

        // When: Incident is created
        Incident incident = new Incident(title, description, status, priority,
                category, requesterId, assigneeId, severity);

        // Then: All fields should be initialized correctly
        assertThat(incident.getTitle()).isEqualTo(title);
        assertThat(incident.getDescription()).isEqualTo(description);
        assertThat(incident.getTicketType()).isEqualTo(TicketType.INCIDENT);
        assertThat(incident.getStatus()).isEqualTo(status);
        assertThat(incident.getPriority()).isEqualTo(priority);
        assertThat(incident.getCategory()).isEqualTo(category);
        assertThat(incident.getRequesterId()).isEqualTo(requesterId);
        assertThat(incident.getAssigneeId()).isEqualTo(assigneeId);
        assertThat(incident.getSeverity()).isEqualTo(severity);
        assertThat(incident.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Constructor should accept null severity")
    void testConstructorWithNullSeverity() {
        // Given: Incident parameters with null severity (not yet assessed)
        Incident incident = new Incident(
                "Test Incident",
                "Description",
                TicketStatus.NEW,
                Priority.MEDIUM,
                "Hardware",
                UUID.randomUUID(),
                null,
                null  // Severity not yet assessed
        );

        // Then: Incident should be created with null severity
        assertThat(incident.getSeverity()).isNull();
        assertThat(incident.getTicketType()).isEqualTo(TicketType.INCIDENT);
    }

    @Test
    @DisplayName("resolve() should set resolution notes and timestamp")
    void testResolve() {
        // Given: An unresolved incident
        Incident incident = createTestIncident();
        assertThat(incident.getResolutionNotes()).isNull();
        assertThat(incident.getResolvedAt()).isNull();

        Instant beforeResolve = Instant.now();

        // When: Incident is resolved
        String resolutionNotes = "Rebooted database server, issue resolved";
        incident.resolve(resolutionNotes);

        // Then: Resolution notes and timestamp should be set
        assertThat(incident.getResolutionNotes()).isEqualTo(resolutionNotes);
        assertThat(incident.getResolvedAt())
                .isNotNull()
                .isAfterOrEqualTo(beforeResolve)
                .isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("updateSeverity() should change severity level")
    void testUpdateSeverity() {
        // Given: An incident with MEDIUM severity
        Incident incident = createTestIncident();
        assertThat(incident.getSeverity()).isEqualTo(Severity.MEDIUM);

        // When: Severity is escalated to CRITICAL
        incident.updateSeverity(Severity.CRITICAL);

        // Then: Severity should be updated
        assertThat(incident.getSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    @DisplayName("Incident should inherit lifecycle callbacks from Ticket")
    void testInheritedLifecycleCallbacks() {
        // Given: A new incident without timestamps
        Incident incident = createTestIncident();
        assertThat(incident.getCreatedAt()).isNull();
        assertThat(incident.getUpdatedAt()).isNull();

        // When: onCreate() lifecycle callback is invoked
        incident.onCreate();

        // Then: Timestamps should be set
        assertThat(incident.getCreatedAt()).isNotNull();
        assertThat(incident.getUpdatedAt()).isNotNull();
        assertThat(incident.getCreatedAt()).isEqualTo(incident.getUpdatedAt());
    }

    @Test
    @DisplayName("Incident should inherit domain methods from Ticket")
    void testInheritedDomainMethods() {
        // Given: An unassigned incident
        Incident incident = createTestIncident();
        assertThat(incident.getAssigneeId()).isNull();

        // When: Incident is assigned to an agent
        UUID agentId = UUID.randomUUID();
        incident.assignTo(agentId);

        // Then: Assignee should be set
        assertThat(incident.getAssigneeId()).isEqualTo(agentId);

        // When: Status is updated
        incident.updateStatus(TicketStatus.IN_PROGRESS);

        // Then: Status should be updated
        assertThat(incident.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    /**
     * Helper method to create a test incident with minimal required fields.
     */
    private Incident createTestIncident() {
        return new Incident(
                "Test Incident",
                "Test Description",
                TicketStatus.NEW,
                Priority.MEDIUM,
                "Test Category",
                UUID.randomUUID(),
                null,
                Severity.MEDIUM
        );
    }
}
