package io.monosense.synergyflow.testsupport.time;

import java.time.Clock;
import java.time.Instant;

/**
 * Default implementation of TimeProvider using the system clock.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class DefaultTimeProvider implements TimeProvider {
    
    private final Clock clock;
    
    public DefaultTimeProvider() {
        this(Clock.systemUTC());
    }
    
    public DefaultTimeProvider(Clock clock) {
        this.clock = clock;
    }
    
    @Override
    public Instant now() {
        return clock.instant();
    }
    
    @Override
    public Clock getClock() {
        return clock;
    }
}