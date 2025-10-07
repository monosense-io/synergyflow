/**
 * ITSM Module - Incident Management and Service Requests
 * <p>
 * This module handles IT Service Management functionality including incident tracking,
 * service request processing, and ticket lifecycle management.
 * </p>
 *
 * @see io.monosense.synergyflow.itsm.api
 * @see io.monosense.synergyflow.itsm.spi
 */
@ApplicationModule(
    displayName = "ITSM Module",
    allowedDependencies = {"security", "eventing", "sla", "sse", "audit"}
)
package io.monosense.synergyflow.itsm;

import org.springframework.modulith.ApplicationModule;
