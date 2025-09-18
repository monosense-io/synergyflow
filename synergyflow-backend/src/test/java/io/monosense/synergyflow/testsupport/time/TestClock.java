package io.monosense.synergyflow.testsupport.time;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Test clock that allows for deterministic time-based testing.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class TestClock extends Clock {
    
    private Instant instant;
    private final ZoneId zoneId;
    
    public TestClock() {
        this(Instant.now(), ZoneId.systemDefault());
    }
    
    public TestClock(Instant instant) {
        this(instant, ZoneId.systemDefault());
    }
    
    public TestClock(Instant instant, ZoneId zoneId) {
        this.instant = instant;
        this.zoneId = zoneId;
    }
    
    @Override
    public ZoneId getZone() {
        return zoneId;
    }
    
    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(instant, zone);
    }
    
    @Override
    public Instant instant() {
        return instant;
    }
    
    /**
     * Advances the clock by the specified amount of time.
     * 
     * @param amount the amount to advance by
     */
    public void advance(java.time.temporal.TemporalAmount amount) {
        instant = instant.plus(amount);
    }
    
    /**
     * Sets the clock to a specific instant.
     * 
     * @param instant the instant to set
     */
    public void setTo(Instant instant) {
        this.instant = instant;
    }
    
    /**
     * Resets the clock to the current system time.
     */
    public void reset() {
        instant = Instant.now();
    }
}