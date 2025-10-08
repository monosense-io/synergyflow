package io.monosense.synergyflow.itsm.integration;

import io.monosense.synergyflow.itsm.internal.domain.*;
import io.monosense.synergyflow.itsm.internal.repository.IncidentRepository;
import io.monosense.synergyflow.itsm.internal.repository.TicketCommentRepository;
import org.junit.jupiter.api.Tag;
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
 * Integration tests for TicketComment entity persistence and queries (Story 2.1 Task 6).
 *
 * <p>Covers:</p>
 * <ul>
 *   <li>IT5: TicketComment persistence and chronological query (findByTicketIdOrderByCreatedAtAsc)</li>
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
class TicketCommentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private void insertUser(UUID userId, String usernamePrefix) {
        String uniqueUsername = usernamePrefix + "-" + userId.toString().substring(0, 8);
        String uniqueEmail = uniqueUsername + "@test.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, username, email, full_name, is_active, version) VALUES (?, ?, ?, ?, ?, ?)",
                userId, uniqueUsername, uniqueEmail, usernamePrefix + " User", true, 1
        );
    }

    @Test
    void it5_ticketCommentPersistenceAndChronologicalQuery() throws Exception {
        // Create a ticket to attach comments to
        UUID requesterId = UUID.randomUUID();
        UUID author1Id = UUID.randomUUID();
        UUID author2Id = UUID.randomUUID();
        UUID author3Id = UUID.randomUUID();

        insertUser(requesterId, "requester");
        insertUser(author1Id, "agent1");
        insertUser(author2Id, "agent2");
        insertUser(author3Id, "agent3");

        Incident ticket = new Incident(
                "Database connection pool exhausted",
                "Application unable to acquire connections",
                TicketStatus.NEW,
                Priority.CRITICAL,
                "Database",
                requesterId,
                null,
                Severity.CRITICAL
        );

        Incident savedTicket = incidentRepository.saveAndFlush(ticket);
        UUID ticketId = savedTicket.getId();

        // Create three comments with time delays to ensure different createdAt timestamps
        TicketComment comment1 = new TicketComment(
                ticketId,
                author1Id,
                "Initial investigation: connection pool maxed out at 50 connections",
                false
        );
        commentRepository.saveAndFlush(comment1);

        Thread.sleep(5); // Small delay to ensure different timestamp

        TicketComment comment2 = new TicketComment(
                ticketId,
                author2Id,
                "Found slow query blocking connections - added index on user_sessions.last_accessed",
                true
        );
        commentRepository.saveAndFlush(comment2);

        Thread.sleep(5); // Small delay to ensure different timestamp

        TicketComment comment3 = new TicketComment(
                ticketId,
                author3Id,
                "Issue resolved - connection pool back to normal levels",
                false
        );
        commentRepository.saveAndFlush(comment3);

        // Query comments by ticket ID in chronological order
        List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        // Verify all three comments returned
        assertThat(comments).hasSize(3);

        // Verify chronological ordering (oldest first)
        assertThat(comments.get(0).getId()).isEqualTo(comment1.getId());
        assertThat(comments.get(0).getCommentText()).contains("Initial investigation");
        assertThat(comments.get(0).isInternal()).isFalse();

        assertThat(comments.get(1).getId()).isEqualTo(comment2.getId());
        assertThat(comments.get(1).getCommentText()).contains("Found slow query");
        assertThat(comments.get(1).isInternal()).isTrue();

        assertThat(comments.get(2).getId()).isEqualTo(comment3.getId());
        assertThat(comments.get(2).getCommentText()).contains("Issue resolved");
        assertThat(comments.get(2).isInternal()).isFalse();

        // Verify timestamps are in chronological order
        assertThat(comments.get(0).getCreatedAt()).isBefore(comments.get(1).getCreatedAt());
        assertThat(comments.get(1).getCreatedAt()).isBefore(comments.get(2).getCreatedAt());

        // Verify all comments reference the same ticket
        assertThat(comments).allMatch(c -> c.getTicketId().equals(ticketId));
    }
}
