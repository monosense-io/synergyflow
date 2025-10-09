package io.monosense.synergyflow.itsm.api.dto;

import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.TicketType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Command object for creating a new ticket in the ITSM system.
 *
 * <p>This record encapsulates all required and optional parameters for ticket creation.
 * It uses Jakarta Bean Validation annotations to enforce field constraints.</p>
 *
 * @param title the descriptive title of the ticket (required, must not be blank)
 * @param description the detailed description of the ticket (required, must not be blank)
 * @param ticketType the type of ticket (INCIDENT or SERVICE_REQUEST) (required)
 * @param priority the priority level (CRITICAL, HIGH, MEDIUM, LOW) (required)
 * @param category the optional category classification for the ticket
 * @param requesterId the unique identifier of the user creating the ticket (required)
 * @since 2.2
 */
public record CreateTicketCommand(
    @NotBlank(message = "Title is required")
    String title,

    @NotBlank(message = "Description is required")
    String description,

    @NotNull(message = "Ticket type is required")
    TicketType ticketType,

    @NotNull(message = "Priority is required")
    Priority priority,

    String category,

    @NotNull(message = "Requester ID is required")
    UUID requesterId
) {
}
