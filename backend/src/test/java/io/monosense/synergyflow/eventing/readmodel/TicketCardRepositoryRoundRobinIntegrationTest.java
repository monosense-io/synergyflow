package io.monosense.synergyflow.eventing.readmodel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@DisplayName("TicketCardRepository Round-Robin Integration Test")
class TicketCardRepositoryRoundRobinIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("synergyflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TicketCardRepository repository;

    UUID user1 = UUID.fromString("00000000-0000-0000-0000-000000000101");
    UUID user2 = UUID.fromString("00000000-0000-0000-0000-000000000102");
    UUID user3 = UUID.fromString("00000000-0000-0000-0000-000000000103");
    UUID teamId = UUID.fromString("00000000-0000-0000-0000-000000000201");

    @BeforeEach
    void seed() {
        // Seed users
        jdbc.update("INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, now(), now(), 1)",
                user1, "u1", "u1@example.com", "User One");
        jdbc.update("INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, now(), now(), 1)",
                user2, "u2", "u2@example.com", "User Two");
        jdbc.update("INSERT INTO users (id, username, email, full_name, is_active, created_at, updated_at, version) VALUES (?, ?, ?, ?, true, now(), now(), 1)",
                user3, "u3", "u3@example.com", "User Three");

        // Seed team and memberships
        jdbc.update("INSERT INTO teams (id, name, description, enabled, created_at, updated_at) VALUES (?, 'RR-TEAM', 'Round robin team', true, now(), now()) ON CONFLICT (name) DO NOTHING",
                teamId);
        jdbc.update("INSERT INTO team_members (id, team_id, user_id, added_at, version) VALUES (gen_random_uuid(), ?, ?, now(), 1)", teamId, user1);
        jdbc.update("INSERT INTO team_members (id, team_id, user_id, added_at, version) VALUES (gen_random_uuid(), ?, ?, now(), 1)", teamId, user2);
        jdbc.update("INSERT INTO team_members (id, team_id, user_id, added_at, version) VALUES (gen_random_uuid(), ?, ?, now(), 1)", teamId, user3);

        // Seed tickets with assignments (counts: u1=3, u2=2, u3=1)
        insertTicket(user1, "NEW");
        insertTicket(user1, "ASSIGNED");
        insertTicket(user1, "IN_PROGRESS");

        insertTicket(user2, "NEW");
        insertTicket(user2, "ASSIGNED");

        insertTicket(user3, "NEW");

        // Closed tickets should be ignored by the query
        insertTicket(user3, "CLOSED");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("TRUNCATE TABLE tickets CASCADE");
        jdbc.update("TRUNCATE TABLE users CASCADE");
    }

    private void insertTicket(UUID assigneeId, String status) {
        jdbc.update("INSERT INTO tickets (id, title, description, status, priority, category, requester_id, assignee_id, ticket_type, created_at, updated_at, version) " +
                        "VALUES (?, 't', 'd', ?, 'HIGH', 'Network', ?, ?, 'INCIDENT', now(), now(), 1)",
                UUID.randomUUID(), status, user1, assigneeId);
    }

    @Test
    @DisplayName("findAgentWithFewestTickets returns UUID of least-loaded agent")
    void picksLeastLoadedAgent() {
        Optional<UUID> agentId = repository.findAgentWithFewestTickets(teamId);
        assertThat(agentId).contains(user3); // user3 has 1 open ticket
    }
}
