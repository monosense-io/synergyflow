/**
 * PM Module - Project Management and Issue Tracking
 * <p>
 * This module handles project management functionality including issue tracking,
 * sprint management, and agile workflow support.
 * </p>
 *
 * @see io.monosense.synergyflow.pm.api
 * @see io.monosense.synergyflow.pm.spi
 */
@ApplicationModule(
    displayName = "PM Module",
    allowedDependencies = {"security", "eventing", "sse", "audit"}
)
package io.monosense.synergyflow.pm;

import org.springframework.modulith.ApplicationModule;
