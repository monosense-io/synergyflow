/**
 * Event Infrastructure Module - Outbox and Event Worker
 * <p>
 * This module provides event publishing infrastructure including transactional outbox pattern,
 * event persistence, and asynchronous event processing. This is a leaf module with no module dependencies.
 * </p>
 *
 * @see io.monosense.synergyflow.eventing.api
 */
@ApplicationModule(displayName = "Event Infrastructure Module")
package io.monosense.synergyflow.eventing;

import org.springframework.modulith.ApplicationModule;
