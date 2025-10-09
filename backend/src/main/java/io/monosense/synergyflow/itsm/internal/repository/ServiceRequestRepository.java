package io.monosense.synergyflow.itsm.internal.repository;

import io.monosense.synergyflow.itsm.internal.domain.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository for ServiceRequest entity persistence.
 *
 * <p>Provides database access operations specific to ServiceRequest entities (a subclass of Ticket).
 * Inherits all standard CRUD operations from JpaRepository and can be extended with
 * ServiceRequest-specific query methods as needed.</p>
 *
 * <p>Service requests represent planned requests for services such as software installation,
 * access requests, or hardware provisioning that require approval and fulfillment tracking.</p>
 *
 * <p>Public visibility required for Spring dependency injection across internal packages.</p>
 *
 * @author monosense
 * @since 2.1
 */
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID> {
    // Inherits findById, save, findAll, delete, etc. from JpaRepository
    // Custom query methods can be added here as needed for ServiceRequest-specific queries
}
