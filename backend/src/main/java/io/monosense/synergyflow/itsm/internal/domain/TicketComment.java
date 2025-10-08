package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style;

/**
 * Represents a comment on a ticket.
 *
 * <p>Comments provide an audit trail and communication history for tickets.
 * They can be either public (visible to requesters) or internal (agent-only).
 * Comments are immutable after creation (append-only model).</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>UUIDv7 primary key for time-ordered identifiers</li>
 *   <li>Indexed foreign key to parent ticket for efficient queries</li>
 *   <li>Automatic timestamp management via {@link #onCreate()} callback</li>
 *   <li>Immutable after creation (no update operations)</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
@Entity
@Table(name = "ticket_comments", indexes = {
        @Index(name = "idx_ticket_comments_ticket_id", columnList = "ticket_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketComment {

    /**
     * UUIDv7 primary key providing time-ordered identifiers.
     *
     * <p>UUIDv7 includes a 48-bit Unix timestamp prefix enabling chronological ordering
     * and improved database index locality. Hibernate 7.0+ generates these automatically.</p>
     */
    @Id
    @UuidGenerator(style = Style.VERSION_7)
    private UUID id;

    /**
     * Foreign key reference to the parent ticket.
     *
     * <p>Indexed for efficient queries retrieving all comments for a specific ticket.</p>
     */
    @NotNull(message = "Ticket ID is required")
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    /**
     * UUID of the user who authored this comment.
     *
     * <p>References a user entity from the authentication module (Keycloak integration).</p>
     */
    @NotNull(message = "Author ID is required")
    @Column(nullable = false)
    private UUID author;

    /**
     * The comment text content.
     *
     * <p>Stored as TEXT column for large content support.</p>
     */
    @NotBlank(message = "Comment text is required")
    @Column(name = "comment_text", columnDefinition = "TEXT", nullable = false)
    private String commentText;

    /**
     * Indicates whether this comment is internal (agent-only).
     *
     * <p>Internal comments are not visible to ticket requesters. Public comments
     * (isInternal=false) are visible to both agents and requesters.</p>
     */
    @Column(name = "is_internal", nullable = false)
    private boolean isInternal;

    /**
     * Timestamp when this comment was created.
     *
     * <p>Set automatically by {@link #onCreate()} callback. Immutable after creation.</p>
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Creates a new ticket comment.
     *
     * <p>Initializes a comment with the specified details. The ID is generated automatically
     * by Hibernate using UUIDv7. The timestamp is managed by the lifecycle callback.</p>
     *
     * @param ticketId the UUID of the parent ticket (required)
     * @param author the UUID of the user authoring the comment (required)
     * @param commentText the comment text content (required)
     * @param isInternal true if this is an internal (agent-only) comment, false for public
     * @since 2.1
     */
    public TicketComment(UUID ticketId, UUID author, String commentText, boolean isInternal) {
        this.ticketId = ticketId;
        this.author = author;
        this.commentText = commentText;
        this.isInternal = isInternal;
    }

    /**
     * JPA lifecycle callback invoked before entity persistence.
     *
     * <p>Sets the {@code createdAt} timestamp to the current instant.</p>
     *
     * @since 2.1
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
