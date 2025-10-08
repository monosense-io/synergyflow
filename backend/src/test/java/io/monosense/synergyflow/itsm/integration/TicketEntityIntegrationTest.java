package io.monosense.synergyflow.itsm.integration;

import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.IncidentRepository;
import io.monosense.synergyflow.itsm.internal.repository.ServiceRequestRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for entity persistence and inheritance mapping (Story 2.1 Tasks 5-6).
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>IT1: Ticket persistence, retrieval, and UUIDv7 time-ordering</li>
 *   <li>IT2: Incident subclass persistence with severity and resolution fields</li>
 *   <li>IT3: ServiceRequest subclass persistence with approval/fulfillment workflow</li>
 *   <li>IT4: Optimistic locking with concurrent updates (ObjectOptimisticLockingFailureException)</li>
 *   <li>IT7: @PrePersist/@PreUpdate lifecycle callbacks for timestamps</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@Testcontainers
@Transactional
@ActiveProfiles("test")
@Tag("integration")
class TicketEntityIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private void insertUser(UUID userId, String usernamePrefix) {
        String uniqueUsername = usernamePrefix + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, usernamePrefix + " User", true, 1
        );
    }

    @Test
    void it1_ticketPersistenceAndUuidV7Ordering() throws Exception {
        UUID requesterId = UUID.randomUUID();
        insertUser(requesterId, "requester");

        Incident t1 = new Incident(
                "Network outage",
                "Users unable to access VPN",
                TicketStatus.NEW,
                Priority.CRITICAL,
                "Network",
                requesterId,
                null,
                Severity.CRITICAL
        );

        Incident saved1 = incidentRepository.saveAndFlush(t1);

        // Ensure different clock tick to strengthen ordering assertion
        Thread.sleep(2L);

        Incident t2 = new Incident(
                "Email latency",
                "Outbound mail queue growing",
                TicketStatus.NEW,
                Priority.HIGH,
                "Messaging",
                requesterId,
                null,
                Severity.HIGH
        );

        Incident saved2 = incidentRepository.saveAndFlush(t2);

        // Basic persistence assertions
        assertThat(saved1.getId()).isNotNull();
        assertThat(saved2.getId()).isNotNull();
        assertThat(saved1.getTicketType()).isEqualTo(TicketType.INCIDENT);
        assertThat(saved2.getTicketType()).isEqualTo(TicketType.INCIDENT);

        // UUIDv7/time-ordered IDs (lexicographic order correlates with creation time)
        String id1 = saved1.getId().toString();
        String id2 = saved2.getId().toString();
        assertThat(id1.compareTo(id2)).isLessThan(0);
    }

    @Test
    void it2_incidentSubclassPersistence() {
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        insertUser(requesterId, "requester");
        insertUser(assigneeId, "agent");

        Incident incident = new Incident(
                "DB connection failures",
                "Spike in timeouts from app to DB",
                TicketStatus.IN_PROGRESS,
                Priority.HIGH,
                "Database",
                requesterId,
                assigneeId,
                Severity.MEDIUM
        );

        Incident saved = incidentRepository.saveAndFlush(incident);

        // Update resolution and persist again
        saved.resolve("Increased connection pool and tuned kernel params");
        Incident updated = incidentRepository.saveAndFlush(saved);

        assertThat(updated.getId()).isNotNull();
        assertThat(updated.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(updated.getResolutionNotes()).contains("connection pool");
        assertThat(updated.getResolvedAt()).isNotNull();
        assertThat(updated.getTicketType()).isEqualTo(TicketType.INCIDENT);
    }

    @Test
    void it3_serviceRequestSubclassPersistence() {
        UUID requesterId = UUID.randomUUID();
        UUID assigneeId = UUID.randomUUID();
        UUID fulfillerId = UUID.randomUUID();
        insertUser(requesterId, "requester");
        insertUser(assigneeId, "agent");
        // fulfiller has no FK, but insert anyway for realism
        insertUser(fulfillerId, "fulfiller");

        ServiceRequest sr = new ServiceRequest(
                "Access to BI tool",
                "Grant Analyst role in BI",
                TicketStatus.NEW,
                Priority.MEDIUM,
                "Access Management",
                requesterId,
                assigneeId,
                "Access Request"
        );

        ServiceRequest saved = serviceRequestRepository.saveAndFlush(sr);
        saved.approve();
        saved.fulfill(fulfillerId);
        ServiceRequest updated = serviceRequestRepository.saveAndFlush(saved);

        assertThat(updated.getId()).isNotNull();
        assertThat(updated.getTicketType()).isEqualTo(TicketType.SERVICE_REQUEST);
        assertThat(updated.getRequestType()).isEqualTo("Access Request");
        assertThat(updated.getApprovalStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(updated.getFulfillerId()).isEqualTo(fulfillerId);
        assertThat(updated.getFulfilledAt()).isNotNull();
    }

    @Test
    void it7_lifecycleCallbacks_setTimestampsOnPersistAndUpdate() throws Exception {
        UUID requesterId = UUID.randomUUID();
        insertUser(requesterId, "requester");

        Incident incident = new Incident(
                "Printer offline",
                "Office printer not reachable",
                TicketStatus.NEW,
                Priority.LOW,
                "Hardware",
                requesterId,
                null,
                null
        );

        Incident saved = incidentRepository.saveAndFlush(incident);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());

        Instant firstUpdated = saved.getUpdatedAt();

        // Ensure a different clock tick before update
        Thread.sleep(2L);
        saved.updateStatus(TicketStatus.IN_PROGRESS);
        Incident updated = incidentRepository.saveAndFlush(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfter(firstUpdated);

        // Sanity: updatedAt and createdAt should be reasonably close
        assertThat(Duration.between(updated.getCreatedAt(), updated.getUpdatedAt()).toMillis())
                .isGreaterThanOrEqualTo(0L);
    }

    @Test
    void it4_optimisticLocking_throwsExceptionOnConcurrentUpdate() {
        UUID requesterId = UUID.randomUUID();
        insertUser(requesterId, "requester");

        // Create and persist initial ticket
        Incident incident = new Incident(
                "Load balancer issue",
                "LB health checks failing",
                TicketStatus.NEW,
                Priority.HIGH,
                "Infrastructure",
                requesterId,
                null,
                Severity.HIGH
        );

        Incident saved = incidentRepository.saveAndFlush(incident);
        UUID ticketId = saved.getId();
        Long initialVersion = saved.getVersion();

        entityManager.clear(); // Detach all entities to simulate fresh session

        // Load the ticket in two separate "sessions" (two references)
        Incident ticket1 = incidentRepository.findById(ticketId).orElseThrow();
        entityManager.detach(ticket1); // Detach first reference

        Incident ticket2 = incidentRepository.findById(ticketId).orElseThrow();
        entityManager.detach(ticket2); // Detach second reference

        // Both references now have version=0 (initial version)
        assertThat(ticket1.getVersion()).isEqualTo(initialVersion);
        assertThat(ticket2.getVersion()).isEqualTo(initialVersion);

        // Transaction 1: Update and save first reference (succeeds, version → 1)
        ticket1.updateStatus(TicketStatus.ASSIGNED);
        Incident updated1 = incidentRepository.saveAndFlush(ticket1);
        assertThat(updated1.getVersion()).isEqualTo(initialVersion + 1);

        // Transaction 2: Try to update and save second reference with stale version
        // This should throw ObjectOptimisticLockingFailureException (Spring wrapper for OptimisticLockException)
        ticket2.updateSeverity(Severity.CRITICAL);
        assertThatThrownBy(() -> incidentRepository.saveAndFlush(ticket2))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Verify the ticket still has the update from transaction 1 and not transaction 2
        Incident finalTicket = incidentRepository.findById(ticketId).orElseThrow();
        assertThat(finalTicket.getStatus()).isEqualTo(TicketStatus.ASSIGNED);
        assertThat(finalTicket.getSeverity()).isEqualTo(Severity.HIGH); // Not CRITICAL from failed update
        assertThat(finalTicket.getVersion()).isEqualTo(initialVersion + 1);
    }
}

