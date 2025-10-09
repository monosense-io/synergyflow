package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TicketCommentRepository custom query methods.
 *
 * <p>Uses Testcontainers with real PostgreSQL container per ADR-009.
 * Tests the chronological ordering of comments by creation timestamp.</p>
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
class TicketCommentRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${test.schema:public}")
    private String testSchema;

    @BeforeEach
    void setSearchPath() {
        jdbcTemplate.execute("SET search_path TO \"" + testSchema + "\"");
    }

    private void ensureUserExists(UUID userId) {
        jdbcTemplate.update(
            "INSERT INTO users (id, username, email, full_name) VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING",
            userId,
            "user_" + userId.toString().substring(0, 8),
            userId.toString().substring(0, 8) + "@example.com",
            "Test User " + userId.toString().substring(0, 8)
        );
    }

    @Test
    void findByTicketIdOrderByCreatedAtAsc_shouldReturnCommentsInChronologicalOrder() throws InterruptedException {
        // Given: Create a parent ticket and comments with sequential timestamps
        UUID author = UUID.randomUUID();
        ensureUserExists(author);

        Ticket ticket = new Ticket(
                "Test ticket",
                "Test description",
                TicketType.INCIDENT,
                TicketStatus.NEW,
                Priority.MEDIUM,
                "General",
                author,
                null
        );
        ticket = ticketRepository.saveAndFlush(ticket);
        UUID ticketId = ticket.getId();

        TicketComment comment1 = new TicketComment(ticketId, author, "First comment", false);
        commentRepository.save(comment1);
        Thread.sleep(10); // Ensure different timestamps

        TicketComment comment2 = new TicketComment(ticketId, author, "Second comment", false);
        commentRepository.save(comment2);
        Thread.sleep(10);

        TicketComment comment3 = new TicketComment(ticketId, author, "Third comment", true);
        commentRepository.save(comment3);

        // When: Query comments for ticket
        List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        // Then: Should return comments in chronological order
        assertThat(comments).hasSize(3);
        assertThat(comments.get(0).getCommentText()).isEqualTo("First comment");
        assertThat(comments.get(1).getCommentText()).isEqualTo("Second comment");
        assertThat(comments.get(2).getCommentText()).isEqualTo("Third comment");

        // Verify timestamps are ascending
        assertThat(comments.get(0).getCreatedAt()).isBefore(comments.get(1).getCreatedAt());
        assertThat(comments.get(1).getCreatedAt()).isBefore(comments.get(2).getCreatedAt());
    }

    @Test
    void findByTicketIdOrderByCreatedAtAsc_shouldReturnEmptyListWhenNoComments() {
        // Given: No comments for ticket
        UUID ticketId = UUID.randomUUID();

        // When: Query comments
        List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        // Then: Should return empty list
        assertThat(comments).isEmpty();
    }

    @Test
    void findByTicketIdOrderByCreatedAtAsc_shouldFilterByTicketId() {
        // Given: Comments for different tickets
        UUID author = UUID.randomUUID();
        ensureUserExists(author);

        Ticket t1 = new Ticket("T1", "D1", TicketType.INCIDENT, TicketStatus.NEW, Priority.LOW, null, author, null);
        Ticket t2 = new Ticket("T2", "D2", TicketType.SERVICE_REQUEST, TicketStatus.NEW, Priority.LOW, null, author, null);
        t1 = ticketRepository.saveAndFlush(t1);
        t2 = ticketRepository.saveAndFlush(t2);

        TicketComment comment1 = new TicketComment(t1.getId(), author, "Ticket 1 comment", false);
        TicketComment comment2 = new TicketComment(t2.getId(), author, "Ticket 2 comment", false);
        TicketComment comment3 = new TicketComment(t1.getId(), author, "Another ticket 1 comment", false);

        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        // When: Query comments for ticket1
        List<TicketComment> ticket1Comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(t1.getId());

        // Then: Should return only ticket1 comments
        assertThat(ticket1Comments).hasSize(2);
        assertThat(ticket1Comments).extracting("commentText")
                .containsExactly("Ticket 1 comment", "Another ticket 1 comment");
    }
}
