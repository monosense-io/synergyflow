package io.monosense.synergyflow.itsm.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Spring Data JPA repository for Ticket aggregate persistence.
 *
 * <p>Provides database access operations for Ticket entities using UUID as the primary key.
 * This repository extends JpaRepository to inherit standard CRUD operations and can be
 * extended with custom query methods as needed.</p>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Service
 * class TicketService {
 *     private final TicketRepository ticketRepository;
 *
 *     public Optional<Ticket> findTicket(UUID id) {
 *         return ticketRepository.findById(id);
 *     }
 *
 *     public List<Ticket> findTicketsByStatus(String status) {
 *         return ticketRepository.findByStatus(status);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
interface TicketRepository extends JpaRepository<Ticket, UUID> {
}
