package io.monosense.synergyflow.itsm.spi;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object providing a summary view of ticket data for cross-module queries.
 *
 * <p>This immutable record exposes essential ticket information without coupling consumers
 * to internal entity implementation details. Used by TicketQueryService to provide read-only
 * access to ticket data from other bounded contexts (PM, Workflow, Dashboard modules).</p>
 *
 * <p><strong>SPI Stability Contract:</strong> This DTO is part of the public SPI. Fields may
 * be added in minor versions (at the end for binary compatibility), but existing fields will
 * not be removed or renamed without a major version bump and deprecation cycle.</p>
 *
 * @param id the unique identifier (UUIDv7) of the ticket
 * @param ticketType the type of ticket (INCIDENT or SERVICE_REQUEST)
 * @param title brief descriptive title of the ticket
 * @param description detailed description of the issue or request
 * @param status current lifecycle status (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
 * @param priority priority level (CRITICAL, HIGH, MEDIUM, LOW)
 * @param category category classification for routing and reporting (nullable)
 * @param requesterId UUID of the user who created the ticket
 * @param assigneeId UUID of the agent assigned to work on the ticket (nullable)
 * @param createdAt timestamp when the ticket was created
 * @param updatedAt timestamp of the last update to the ticket
 *
 * @since 2.2
 */
public record TicketSummaryDTO(
        UUID id,
        TicketType ticketType,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        String category,
        UUID requesterId,
        UUID assigneeId,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Factory method to create a TicketSummaryDTO from an internal Ticket entity.
     *
     * <p>Maps internal domain enums to SPI enums to maintain module boundary separation.
     * This method is the canonical way to convert internal entities to cross-module DTOs.</p>
     *
     * @param ticket the internal Ticket entity to convert
     * @return a new TicketSummaryDTO with data from the ticket entity
     * @throws NullPointerException if ticket is null
     * @since 2.2
     */
    public static TicketSummaryDTO from(io.monosense.synergyflow.itsm.internal.domain.Ticket ticket) {
        if (ticket == null) {
            throw new NullPointerException("Ticket cannot be null");
        }

        return new TicketSummaryDTO(
                ticket.getId(),
                mapTicketType(ticket.getTicketType()),
                ticket.getTitle(),
                ticket.getDescription(),
                mapStatus(ticket.getStatus()),
                mapPriority(ticket.getPriority()),
                ticket.getCategory(),
                ticket.getRequesterId(),
                ticket.getAssigneeId(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    /**
     * Maps internal TicketType enum to SPI TicketType enum.
     */
    private static TicketType mapTicketType(io.monosense.synergyflow.itsm.internal.domain.TicketType internal) {
        if (internal == null) {
            return null;
        }
        return TicketType.valueOf(internal.name());
    }

    /**
     * Maps internal TicketStatus enum to SPI TicketStatus enum.
     */
    private static TicketStatus mapStatus(io.monosense.synergyflow.itsm.internal.domain.TicketStatus internal) {
        if (internal == null) {
            return null;
        }
        return TicketStatus.valueOf(internal.name());
    }

    /**
     * Maps internal Priority enum to SPI Priority enum.
     */
    private static Priority mapPriority(io.monosense.synergyflow.itsm.internal.domain.Priority internal) {
        if (internal == null) {
            return null;
        }
        return Priority.valueOf(internal.name());
    }
}
