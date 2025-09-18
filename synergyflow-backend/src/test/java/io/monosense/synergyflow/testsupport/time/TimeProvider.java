package io.monosense.synergyflow.testsupport.time;

import java.time.Clock;
import java.time.Instant;

/**
 * Provider for current time, allowing for deterministic testing.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public interface TimeProvider {
    
    /**
     * Gets the current instant.
     * 
     * @return the current instant
     */
    Instant now();
    
    /**
     * Gets the underlying clock.
     * 
     * @return the clock
     */
    Clock getClock();
}