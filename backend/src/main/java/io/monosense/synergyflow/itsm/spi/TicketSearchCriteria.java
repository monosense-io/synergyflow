package io.monosense.synergyflow.itsm.spi;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Search criteria for dynamic ticket queries.
 *
 * <p>Immutable record containing optional filter criteria for ticket searches. All fields are
 * wrapped in {@link Optional} to distinguish between "not specified" (Optional.empty()) and
 * explicit values. Use the {@link Builder} to construct instances with a fluent API.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * TicketSearchCriteria criteria = TicketSearchCriteria.builder()
 *     .status(TicketStatus.ASSIGNED)
 *     .priority(Priority.HIGH)
 *     .assigneeId(agentUuid)
 *     .build();
 * </pre>
 *
 * <p><strong>SPI Stability Contract:</strong> This DTO is part of the public SPI. New filter
 * fields may be added in minor versions. Existing fields will not be removed or renamed without
 * a major version bump and deprecation cycle.</p>
 *
 * @param status filter by ticket status (NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED)
 * @param priority filter by priority level (CRITICAL, HIGH, MEDIUM, LOW)
 * @param category filter by category classification
 * @param assigneeId filter by UUID of assigned agent
 * @param requesterId filter by UUID of requester
 * @param createdAfter filter by minimum creation timestamp (inclusive)
 * @param createdBefore filter by maximum creation timestamp (exclusive)
 *
 * @since 2.2
 */
public record TicketSearchCriteria(
        Optional<TicketStatus> status,
        Optional<Priority> priority,
        Optional<String> category,
        Optional<UUID> assigneeId,
        Optional<UUID> requesterId,
        Optional<Instant> createdAfter,
        Optional<Instant> createdBefore
) {

    /**
     * Creates a new builder for constructing TicketSearchCriteria instances.
     *
     * @return a new Builder instance with all fields initially empty
     * @since 2.2
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing TicketSearchCriteria with a fluent API.
     *
     * <p>All fields are optional. Only the criteria explicitly set via builder methods
     * will be included in the resulting query.</p>
     *
     * @since 2.2
     */
    public static class Builder {
        private Optional<TicketStatus> status = Optional.empty();
        private Optional<Priority> priority = Optional.empty();
        private Optional<String> category = Optional.empty();
        private Optional<UUID> assigneeId = Optional.empty();
        private Optional<UUID> requesterId = Optional.empty();
        private Optional<Instant> createdAfter = Optional.empty();
        private Optional<Instant> createdBefore = Optional.empty();

        /**
         * Sets the status filter criterion.
         *
         * @param status the ticket status to filter by
         * @return this builder for method chaining
         */
        public Builder status(TicketStatus status) {
            this.status = Optional.ofNullable(status);
            return this;
        }

        /**
         * Sets the priority filter criterion.
         *
         * @param priority the priority level to filter by
         * @return this builder for method chaining
         */
        public Builder priority(Priority priority) {
            this.priority = Optional.ofNullable(priority);
            return this;
        }

        /**
         * Sets the category filter criterion.
         *
         * @param category the category classification to filter by
         * @return this builder for method chaining
         */
        public Builder category(String category) {
            this.category = Optional.ofNullable(category);
            return this;
        }

        /**
         * Sets the assignee ID filter criterion.
         *
         * @param assigneeId the UUID of the assigned agent to filter by
         * @return this builder for method chaining
         */
        public Builder assigneeId(UUID assigneeId) {
            this.assigneeId = Optional.ofNullable(assigneeId);
            return this;
        }

        /**
         * Sets the requester ID filter criterion.
         *
         * @param requesterId the UUID of the requester to filter by
         * @return this builder for method chaining
         */
        public Builder requesterId(UUID requesterId) {
            this.requesterId = Optional.ofNullable(requesterId);
            return this;
        }

        /**
         * Sets the minimum creation timestamp filter criterion (inclusive).
         *
         * @param createdAfter tickets created on or after this timestamp
         * @return this builder for method chaining
         */
        public Builder createdAfter(Instant createdAfter) {
            this.createdAfter = Optional.ofNullable(createdAfter);
            return this;
        }

        /**
         * Sets the maximum creation timestamp filter criterion (exclusive).
         *
         * @param createdBefore tickets created before this timestamp
         * @return this builder for method chaining
         */
        public Builder createdBefore(Instant createdBefore) {
            this.createdBefore = Optional.ofNullable(createdBefore);
            return this;
        }

        /**
         * Builds the TicketSearchCriteria instance with the configured filters.
         *
         * @return a new immutable TicketSearchCriteria record
         */
        public TicketSearchCriteria build() {
            return new TicketSearchCriteria(
                    status,
                    priority,
                    category,
                    assigneeId,
                    requesterId,
                    createdAfter,
                    createdBefore
            );
        }
    }
}
