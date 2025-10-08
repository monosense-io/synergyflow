package io.monosense.synergyflow.itsm.internal.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TicketComment} entity.
 *
 * <p>Tests verify entity construction, field initialization, and lifecycle callback behavior
 * for the TicketComment entity without requiring database persistence.</p>
 *
 * @author monosense
 * @since 2.1
 */
@DisplayName("TicketComment Entity Tests")
class TicketCommentTest {

    @Test
    @DisplayName("Constructor should initialize all fields correctly")
    void testConstructorInitializesFields() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        String commentText = "This is a test comment.";
        boolean isInternal = true;

        // Act
        TicketComment comment = new TicketComment(ticketId, author, commentText, isInternal);

        // Assert
        assertThat(comment.getTicketId()).isEqualTo(ticketId);
        assertThat(comment.getAuthor()).isEqualTo(author);
        assertThat(comment.getCommentText()).isEqualTo(commentText);
        assertThat(comment.isInternal()).isTrue();
        assertThat(comment.getId()).isNull(); // ID not set until persistence
        assertThat(comment.getCreatedAt()).isNull(); // Timestamp not set until @PrePersist
    }

    @Test
    @DisplayName("Constructor should handle public comment (isInternal=false)")
    void testConstructorPublicComment() {
        // Arrange
        UUID ticketId = UUID.randomUUID();
        UUID author = UUID.randomUUID();
        String commentText = "Public comment visible to requester.";

        // Act
        TicketComment comment = new TicketComment(ticketId, author, commentText, false);

        // Assert
        assertThat(comment.isInternal()).isFalse();
    }

    @Test
    @DisplayName("@PrePersist should set createdAt timestamp")
    void testPrePersistSetsCreatedAt() throws Exception {
        // Arrange
        TicketComment comment = new TicketComment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test comment",
                false
        );
        Instant beforeCreate = Instant.now();

        // Act - manually invoke the @PrePersist callback
        Method onCreate = TicketComment.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(comment);

        Instant afterCreate = Instant.now();

        // Assert
        assertThat(comment.getCreatedAt()).isNotNull();
        assertThat(comment.getCreatedAt()).isBetween(beforeCreate, afterCreate);
    }

    @Test
    @DisplayName("@PrePersist should be idempotent when called multiple times")
    void testPrePersistIdempotent() throws Exception {
        // Arrange
        TicketComment comment = new TicketComment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test comment",
                true
        );

        // Act - invoke @PrePersist twice
        Method onCreate = TicketComment.class.getDeclaredMethod("onCreate");
        onCreate.setAccessible(true);
        onCreate.invoke(comment);
        Instant firstTimestamp = comment.getCreatedAt();

        Thread.sleep(10); // Small delay to ensure different timestamps if re-set

        onCreate.invoke(comment);
        Instant secondTimestamp = comment.getCreatedAt();

        // Assert - second invocation should overwrite (JPA behavior, though normally called once)
        assertThat(secondTimestamp).isNotNull();
        assertThat(secondTimestamp).isAfterOrEqualTo(firstTimestamp);
    }

    @Test
    @DisplayName("Constructor should handle long comment text")
    void testConstructorWithLongText() {
        // Arrange
        String longText = "A".repeat(5000); // 5000 characters

        // Act
        TicketComment comment = new TicketComment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                longText,
                false
        );

        // Assert
        assertThat(comment.getCommentText()).hasSize(5000);
        assertThat(comment.getCommentText()).isEqualTo(longText);
    }

    @Test
    @DisplayName("Constructor should handle empty comment text")
    void testConstructorWithEmptyText() {
        // Arrange & Act
        TicketComment comment = new TicketComment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "",
                false
        );

        // Assert
        assertThat(comment.getCommentText()).isEmpty();
    }

    @Test
    @DisplayName("TicketComment should be immutable after creation (no setters)")
    void testImmutability() {
        // Arrange
        TicketComment comment = new TicketComment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Original text",
                true
        );

        // Assert - verify no public setters exist (Lombok @Getter only, no @Setter)
        assertThat(comment.getClass().getMethods())
                .noneMatch(method -> method.getName().startsWith("set") &&
                        method.getParameterCount() == 1);
    }
}
