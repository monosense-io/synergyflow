package io.monosense.synergyflow.itsm.internal.service;

import io.monosense.synergyflow.itsm.internal.domain.Priority;
import io.monosense.synergyflow.itsm.internal.domain.Ticket;
import io.monosense.synergyflow.itsm.internal.domain.TicketStatus;
import io.monosense.synergyflow.itsm.internal.repository.TicketRepository;
import io.monosense.synergyflow.itsm.spi.TicketQueryService;
import io.monosense.synergyflow.itsm.spi.TicketSearchCriteria;
import io.monosense.synergyflow.itsm.spi.TicketSummaryDTO;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of the TicketQueryService SPI for cross-module ticket queries.
 *
 * <p>Provides read-only access to ticket data for other bounded contexts (PM, Workflow, Dashboard modules).
 * All queries are optimized with read-only transactions and convert internal entities to SPI DTOs
 * to maintain module boundary separation.</p>
 *
 * <p>Package-private visibility ensures this implementation detail is not exposed outside the
 * ITSM module. External modules interact exclusively through the public TicketQueryService interface.</p>
 *
 * <p><strong>Performance Optimizations:</strong></p>
 * <ul>
 *   <li>{@code @Transactional(readOnly=true)} enables Hibernate read-only optimizations</li>
 *   <li>{@code @Cacheable} on findById() reduces database load for frequently accessed tickets</li>
 *   <li>Results sorted by creation time (newest first) for optimal user experience</li>
 * </ul>
 *
 * @author monosense
 * @since 2.2
 */
@Service
@Transactional(readOnly = true)
class TicketQueryServiceImpl implements TicketQueryService {

    private final TicketRepository ticketRepository;

    /**
     * Constructs the service with required dependencies.
     *
     * @param ticketRepository the repository for ticket data access
     */
    public TicketQueryServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cached to reduce database load for frequently accessed tickets.
     * Cache key is the ticket UUID. Both present and empty results are cached.</p>
     */
    @Override
    @Cacheable(value = "tickets", key = "#ticketId", condition = "#ticketId != null")
    public Optional<TicketSummaryDTO> findById(UUID ticketId) {
        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId cannot be null");
        }

        return ticketRepository.findById(ticketId)
                .map(TicketSummaryDTO::from);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TicketSummaryDTO> findByAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("assigneeId cannot be null");
        }

        return ticketRepository.findByAssigneeId(assigneeId).stream()
                .map(TicketSummaryDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TicketSummaryDTO> findByRequester(UUID requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("requesterId cannot be null");
        }

        return ticketRepository.findByRequesterId(requesterId).stream()
                .map(TicketSummaryDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TicketSummaryDTO> findByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        // Map SPI enum to internal enum
        TicketStatus internalStatus = TicketStatus.valueOf(status.name());

        return ticketRepository.findByStatus(internalStatus).stream()
                .map(TicketSummaryDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TicketSummaryDTO> findByPriority(io.monosense.synergyflow.itsm.spi.Priority priority) {
        if (priority == null) {
            throw new IllegalArgumentException("priority cannot be null");
        }

        // Map SPI enum to internal enum
        Priority internalPriority = Priority.valueOf(priority.name());

        // Use Specification for consistency with findByCriteria
        Specification<Ticket> spec = TicketSpecifications.hasPriority(internalPriority);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        return ticketRepository.findAll(spec, sort).stream()
                .map(TicketSummaryDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses Spring Data JPA Specification for dynamic query construction based on
     * non-empty criteria fields. All criteria are combined with AND logic.</p>
     */
    @Override
    public List<TicketSummaryDTO> findByCriteria(TicketSearchCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("criteria cannot be null");
        }

        Specification<Ticket> spec = TicketSpecifications.fromCriteria(criteria);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        return ticketRepository.findAll(spec, sort).stream()
                .map(TicketSummaryDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean existsById(UUID ticketId) {
        if (ticketId == null) {
            throw new IllegalArgumentException("ticketId cannot be null");
        }

        return ticketRepository.existsById(ticketId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByStatus(io.monosense.synergyflow.itsm.spi.TicketStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status cannot be null");
        }

        // Map SPI enum to internal enum
        TicketStatus internalStatus = TicketStatus.valueOf(status.name());

        // Use Specification for consistency
        Specification<Ticket> spec = TicketSpecifications.hasStatus(internalStatus);

        return ticketRepository.count(spec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long countByAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("assigneeId cannot be null");
        }

        // Use Specification for consistency
        Specification<Ticket> spec = TicketSpecifications.hasAssignee(assigneeId);

        return ticketRepository.count(spec);
    }
}
