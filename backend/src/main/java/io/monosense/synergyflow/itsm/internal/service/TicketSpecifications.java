package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.spi.TicketSearchCriteria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Utility class for building JPA Specifications for Ticket queries.
 *
 * <p>Provides static factory methods to create Specification instances for dynamic
 * ticket queries. Used by TicketQueryServiceImpl to implement the findByCriteria()
 * method with complex filter combinations.</p>
 *
 * <p>Package-private visibility maintains encapsulation within the service layer.</p>
 *
 * @author monosense
 * @since 2.2
 */
class TicketSpecifications {

    private TicketSpecifications() {
        // Utility class, prevent instantiation
    }

    /**
     * Creates a Specification from TicketSearchCriteria.
     *
     * <p>Converts SPI criteria to internal JPA Specification, combining all
     * non-empty criteria with AND logic. Maps SPI enums to internal enums.</p>
     *
     * @param criteria the search criteria from the SPI
     * @return a Specification combining all specified filters
     */
    static Specification<Ticket> fromCriteria(TicketSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Status filter
            criteria.status().ifPresent(spiStatus -> {
                TicketStatus internalStatus = TicketStatus.valueOf(spiStatus.name());
                predicates.add(criteriaBuilder.equal(root.get("status"), internalStatus));
            });

            // Priority filter
            criteria.priority().ifPresent(spiPriority -> {
                Priority internalPriority = Priority.valueOf(spiPriority.name());
                predicates.add(criteriaBuilder.equal(root.get("priority"), internalPriority));
            });

            // Category filter
            criteria.category().ifPresent(category ->
                    predicates.add(criteriaBuilder.equal(root.get("category"), category))
            );

            // Assignee ID filter
            criteria.assigneeId().ifPresent(assigneeId ->
                    predicates.add(criteriaBuilder.equal(root.get("assigneeId"), assigneeId))
            );

            // Requester ID filter
            criteria.requesterId().ifPresent(requesterId ->
                    predicates.add(criteriaBuilder.equal(root.get("requesterId"), requesterId))
            );

            // Created after filter (inclusive)
            criteria.createdAfter().ifPresent(createdAfter ->
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter))
            );

            // Created before filter (exclusive)
            criteria.createdBefore().ifPresent(createdBefore ->
                    predicates.add(criteriaBuilder.lessThan(root.get("createdAt"), createdBefore))
            );

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a Specification for filtering by status.
     *
     * @param status the internal TicketStatus to filter by
     * @return a Specification matching the status
     */
    static Specification<Ticket> hasStatus(TicketStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);
    }

    /**
     * Creates a Specification for filtering by priority.
     *
     * @param priority the internal Priority to filter by
     * @return a Specification matching the priority
     */
    static Specification<Ticket> hasPriority(Priority priority) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("priority"), priority);
    }

    /**
     * Creates a Specification for filtering by assignee ID.
     *
     * @param assigneeId the UUID of the assigned agent
     * @return a Specification matching the assignee
     */
    static Specification<Ticket> hasAssignee(UUID assigneeId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("assigneeId"), assigneeId);
    }

    /**
     * Creates a Specification for filtering by requester ID.
     *
     * @param requesterId the UUID of the requester
     * @return a Specification matching the requester
     */
    static Specification<Ticket> hasRequester(UUID requesterId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("requesterId"), requesterId);
    }
}
