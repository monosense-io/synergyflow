/**
 * Workflow Module - Approvals and State Machines
 * <p>
 * This module orchestrates cross-module workflows, approval processes, and state transitions
 * for both ITSM and PM domains. Depends on SPI interfaces only, not internal implementations.
 * </p>
 *
 * @see io.monosense.synergyflow.workflow.api
 */
@ApplicationModule(
    displayName = "Workflow Module",
    allowedDependencies = {"itsm :: spi", "pm :: spi", "security", "eventing", "audit"}
)
package io.monosense.synergyflow.workflow;

import org.springframework.modulith.ApplicationModule;
