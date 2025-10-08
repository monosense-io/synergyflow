package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.Incident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for Incident entity persistence.
 *
 * <p>Provides database access operations specific to Incident entities (a subclass of Ticket).
 * Inherits all standard CRUD operations from JpaRepository and can be extended with
 * Incident-specific query methods as needed.</p>
 *
 * <p>Incidents represent unplanned interruptions or reductions in service quality that require
 * immediate attention and resolution tracking.</p>
 *
 * <p>Public visibility required for Spring dependency injection across internal packages.</p>
 *
 * @author monosense
 * @since 2.1
 */
public interface IncidentRepository extends JpaRepository<Incident, UUID> {
    // Inherits findById, save, findAll, delete, etc. from JpaRepository
    // Custom query methods can be added here as needed for Incident-specific queries
}
