/**
 * Incident Management Module (ITSM).
 *
 * <p>Handles incident lifecycle: creation, assignment, resolution, and closure.
 * Publishes domain events via Spring Modulith ApplicationEvents.
 *
 * <h2>Public API:</h2>
 * <ul>
 *   <li>{@link io.monosense.synergyflow.incident.api.IncidentCreatedEvent} - Published when incident created</li>
 *   <li>{@link io.monosense.synergyflow.incident.api.IncidentDTO} - Data transfer object</li>
 * </ul>
 *
 * <h2>Module Boundaries:</h2>
 * <p>This module does NOT directly access other module databases.
 * Cross-module communication happens via events only.
 *
 * @see org.springframework.modulith.ApplicationModule
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Incident Management",
    allowedDependencies = "common"
)
package io.monosense.synergyflow.incident;
