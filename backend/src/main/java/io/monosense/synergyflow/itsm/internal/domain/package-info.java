/**
 * Internal domain model for ITSM ticket management.
 *
 * <p>This package contains the core domain entities, value objects, and enums
 * for the IT Service Management module. All types are package-private to enforce
 * encapsulation per Spring Modulith conventions.</p>
 *
 * <p><strong>Key Entities:</strong></p>
 * <ul>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.Ticket} - Base ticket aggregate with UUIDv7 IDs</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.Incident} - Incident ticket subclass (future)</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.ServiceRequest} - Service request subclass (future)</li>
 * </ul>
 *
 * <p><strong>Domain Enumerations:</strong></p>
 * <ul>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.TicketType} - Ticket type discriminator</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.TicketStatus} - Ticket lifecycle states</li>
 *   <li>{@link io.monosense.synergyflow.itsm.internal.domain.Priority} - Priority levels</li>
 * </ul>
 *
 * <p><strong>Architecture Notes:</strong></p>
 * <ul>
 *   <li>All entities use UUIDv7 primary keys for time-ordered IDs (6.6x faster inserts vs UUIDv4)</li>
 *   <li>Optimistic locking via {@code @Version} prevents lost updates</li>
 *   <li>Timestamps managed via {@code @PrePersist} and {@code @PreUpdate} callbacks</li>
 *   <li>SINGLE_TABLE inheritance strategy for performance (read-heavy workload)</li>
 * </ul>
 *
 * @see <a href="file:../../../../../../../../docs/architecture/module-boundaries.md">Module Boundaries</a>
 * @see <a href="file:../../../../../../../../docs/uuidv7-implementation-guide.md">UUIDv7 Implementation Guide</a>
 * @since 2.1
 */
package io.monosense.synergyflow.itsm.internal.domain;
