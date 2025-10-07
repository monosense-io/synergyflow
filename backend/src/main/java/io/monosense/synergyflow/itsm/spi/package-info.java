/**
 * ITSM Service Provider Interface - Exposes ticket query APIs for other modules
 * <p>
 * This package contains SPI contracts that allow other modules (e.g., workflow) to query
 * ITSM tickets and status information without depending on internal implementations.
 * </p>
 * <p>
 * NOTE: This is a placeholder for Story 1.2. Actual SPI interfaces (e.g., TicketQueryService)
 * will be implemented in Story 1.3+ when domain entities exist.
 * </p>
 */
@NamedInterface("spi")
package io.monosense.synergyflow.itsm.spi;

import org.springframework.modulith.NamedInterface;
