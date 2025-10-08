package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.TicketComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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

    @Test
    void findByTicketIdOrderByCreatedAtAsc_shouldReturnCommentsInChronologicalOrder() throws InterruptedException {
        // Given: Create comments with sequential timestamps
        UUID ticketId = UUID.randomUUID();
        UUID author = UUID.randomUUID();

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
        UUID ticket1 = UUID.randomUUID();
        UUID ticket2 = UUID.randomUUID();
        UUID author = UUID.randomUUID();

        TicketComment comment1 = new TicketComment(ticket1, author, "Ticket 1 comment", false);
        TicketComment comment2 = new TicketComment(ticket2, author, "Ticket 2 comment", false);
        TicketComment comment3 = new TicketComment(ticket1, author, "Another ticket 1 comment", false);

        commentRepository.save(comment1);
        commentRepository.save(comment2);
        commentRepository.save(comment3);

        // When: Query comments for ticket1
        List<TicketComment> ticket1Comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket1);

        // Then: Should return only ticket1 comments
        assertThat(ticket1Comments).hasSize(2);
        assertThat(ticket1Comments).extracting("commentText")
                .containsExactly("Ticket 1 comment", "Another ticket 1 comment");
    }
}
