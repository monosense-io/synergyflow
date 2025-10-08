/**
 * Internal repository layer for ITSM module data access.
 *
 * <p>This package contains Spring Data JPA repository interfaces for persisting and querying
 * ITSM domain entities. All repositories follow Spring Modulith conventions with package-private
 * visibility to enforce module boundaries.</p>
 *
 * <p>Key characteristics:</p>
 * <ul>
 *   <li>Package-private visibility (implementation detail of ITSM module)</li>
 *   <li>Spring Data JPA repositories extending {@link org.springframework.data.jpa.repository.JpaRepository}</li>
 *   <li>Custom query methods using Spring Data query derivation</li>
 *   <li>UUIDv7 primary keys for all entities</li>
 * </ul>
 *
 * <p>Available repositories:</p>
 * <ul>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.repository.TicketRepository} - Base ticket persistence and queries</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.repository.IncidentRepository} - Incident-specific operations</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.repository.ServiceRequestRepository} - Service request operations</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.repository.TicketCommentRepository} - Comment persistence and chronological queries</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.repository.RoutingRuleRepository} - Routing rule queries</li>
 * </ul>
 *
 * @author monosense
 * @since 2.1
 */
package io.monosense.synergyflow.itsm.internal.repository;
