package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TicketRepository custom query methods.
 *
 * <p>Uses Testcontainers with real PostgreSQL container per ADR-009.
 * Tests verify custom query method behavior with actual database interactions.</p>
 *
 * @author monosense
 * @since 2.1
 */
@SpringBootTest(properties = {
        "spring.data.redis.repositories.enabled=false",
        "spring.redis.host=localhost",
        "spring.redis.port=6379"
})
@Testcontainers
@Transactional
@ActiveProfiles("test")
class TicketRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Helper method to insert a test user into the database.
     * Required to satisfy foreign key constraints on tickets.requester_id.
     */
    private void insertUser(UUID userId, String usernamePrefix) {
        String uniqueUsername = usernamePrefix + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, usernamePrefix + " User", true, 1
        );
    }

    @Test
    void findByStatus_shouldReturnTicketsWithMatchingStatus() {
        // Given: Create tickets with different statuses
        UUID requesterId = UUID.randomUUID();
        insertUser(requesterId, "requester");
        Incident incident1 = new Incident("Incident 1", "Description 1", TicketStatus.NEW,
                Priority.HIGH, "Network", requesterId, null, Severity.CRITICAL);
        Incident incident2 = new Incident("Incident 2", "Description 2", TicketStatus.IN_PROGRESS,
                Priority.MEDIUM, "Network", requesterId, null, Severity.HIGH);
        Incident incident3 = new Incident("Incident 3", "Description 3", TicketStatus.NEW,
                Priority.LOW, "Software", requesterId, null, Severity.LOW);

        ticketRepository.save(incident1);
        ticketRepository.save(incident2);
        ticketRepository.save(incident3);

        // When: Query tickets with status NEW
        List<Ticket> newTickets = ticketRepository.findByStatus(TicketStatus.NEW);

        // Then: Should return only tickets with NEW status
        assertThat(newTickets).hasSize(2);
        assertThat(newTickets).extracting("title")
                .containsExactlyInAnyOrder("Incident 1", "Incident 3");
        assertThat(newTickets).allMatch(ticket -> ticket.getStatus() == TicketStatus.NEW);
    }

    @Test
    void findByRequesterId_shouldReturnTicketsFromSpecificRequester() {
        // Given: Create tickets from different requesters
        UUID requester1 = UUID.randomUUID();
        UUID requester2 = UUID.randomUUID();
        insertUser(requester1, "requester1");
        insertUser(requester2, "requester2");

        Incident ticket1 = new Incident("Ticket 1", "Desc 1", TicketStatus.NEW,
                Priority.HIGH, "Network", requester1, null, null);
        Incident ticket2 = new Incident("Ticket 2", "Desc 2", TicketStatus.NEW,
                Priority.MEDIUM, "Software", requester2, null, null);
        Incident ticket3 = new Incident("Ticket 3", "Desc 3", TicketStatus.NEW,
                Priority.LOW, "Hardware", requester1, null, null);

        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);
        ticketRepository.save(ticket3);

        // When: Query tickets from requester1
        List<Ticket> requester1Tickets = ticketRepository.findByRequesterId(requester1);

        // Then: Should return only tickets from requester1
        assertThat(requester1Tickets).hasSize(2);
        assertThat(requester1Tickets).extracting("title")
                .containsExactlyInAnyOrder("Ticket 1", "Ticket 3");
        assertThat(requester1Tickets).allMatch(ticket -> ticket.getRequesterId().equals(requester1));
    }

    @Test
    void findByAssigneeId_shouldReturnTicketsAssignedToSpecificAgent() {
        // Given: Create tickets assigned to different agents
        UUID requester = UUID.randomUUID();
        UUID agent1 = UUID.randomUUID();
        UUID agent2 = UUID.randomUUID();
        insertUser(requester, "requester");
        insertUser(agent1, "agent1");
        insertUser(agent2, "agent2");

        Incident ticket1 = new Incident("Ticket 1", "Desc 1", TicketStatus.ASSIGNED,
                Priority.HIGH, "Network", requester, agent1, null);
        Incident ticket2 = new Incident("Ticket 2", "Desc 2", TicketStatus.ASSIGNED,
                Priority.MEDIUM, "Software", requester, agent2, null);
        Incident ticket3 = new Incident("Ticket 3", "Desc 3", TicketStatus.NEW,
                Priority.LOW, "Hardware", requester, null, null); // Unassigned

        ticketRepository.save(ticket1);
        ticketRepository.save(ticket2);
        ticketRepository.save(ticket3);

        // When: Query tickets assigned to agent1
        List<Ticket> agent1Tickets = ticketRepository.findByAssigneeId(agent1);

        // Then: Should return only tickets assigned to agent1
        assertThat(agent1Tickets).hasSize(1);
        assertThat(agent1Tickets.get(0).getTitle()).isEqualTo("Ticket 1");
        assertThat(agent1Tickets.get(0).getAssigneeId()).isEqualTo(agent1);
    }

    @Test
    void findByAssigneeId_shouldReturnEmptyListWhenNoTicketsAssigned() {
        // Given: No tickets in database (but user exists for FK)
        UUID agent = UUID.randomUUID();
        insertUser(agent, "agent");

        // When: Query tickets assigned to agent
        List<Ticket> tickets = ticketRepository.findByAssigneeId(agent);

        // Then: Should return empty list
        assertThat(tickets).isEmpty();
    }
}
