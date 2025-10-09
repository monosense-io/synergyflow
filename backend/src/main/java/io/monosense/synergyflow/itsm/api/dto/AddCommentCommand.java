package io.monosense.synergyflow.itsm.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command object for adding a comment to a ticket.
 *
 * <p>This record encapsulates the parameters required for creating a new comment on a ticket.
 * Comments can be either public (visible to requesters) or internal (visible only to agents).</p>
 *
 * @param commentText the text content of the comment (required, minimum 1 character)
 * @param isInternal whether the comment is internal-only (defaults to false for public comments)
 * @since 2.2
 */
public record AddCommentCommand(
    @NotBlank(message = "Comment text is required")
    @Size(min = 1, message = "Comment text must have at least 1 character")
    String commentText,

    Boolean isInternal
) {
    /**
     * Canonical constructor with default value for isInternal.
     */
    public AddCommentCommand {
        if (isInternal == null) {
            isInternal = false;
        }
    }
}
