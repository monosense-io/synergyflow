package io.monosense.synergyflow.testsupport.events;

import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for testing event publications in Spring Modulith applications.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
public class EventTestHelper {
    
    /**
     * Asserts that an event of the specified type was published.
     * 
     * @param events the published events
     * @param eventType the expected event type
     * @param <T> the event type
     */
    public static <T> void assertEventPublished(PublishedEvents events, Class<T> eventType) {
        assertThat(events.ofType(eventType)).isNotEmpty();
    }
    
    /**
     * Asserts that an event of the specified type was published and matches the given criteria.
     * 
     * @param events the published events
     * @param eventType the expected event type
     * @param matcher the criteria to match
     * @param <T> the event type
     */
    public static <T> void assertEventPublished(PublishedEvents events, Class<T> eventType, java.util.function.Predicate<T> matcher) {
        assertThat(events.ofType(eventType)).anyMatch(matcher);
    }
    
    /**
     * Asserts that an event of the specified type was published using AssertablePublishedEvents.
     * 
     * @param events the assertable published events
     * @param eventType the expected event type
     * @param <T> the event type
     */
    public static <T> void assertEventPublished(AssertablePublishedEvents events, Class<T> eventType) {
        assertThat(events).contains(eventType);
    }
    
    /**
     * Asserts that an event of the specified type was published and matches the given criteria using AssertablePublishedEvents.
     * 
     * @param events the assertable published events
     * @param eventType the expected event type
     * @param matcher the criteria to match
     * @param <T> the event type
     */
    public static <T> void assertEventPublished(AssertablePublishedEvents events, Class<T> eventType, java.util.function.Predicate<T> matcher) {
        assertThat(events).contains(eventType).matching(matcher);
    }
}