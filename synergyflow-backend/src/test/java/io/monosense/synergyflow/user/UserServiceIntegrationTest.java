package io.monosense.synergyflow.user;

import io.monosense.synergyflow.testsupport.events.EventTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;
import org.springframework.modulith.test.PublishedEvents;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example integration test demonstrating event publication verification.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@ApplicationModuleTest
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void shouldPublishUserCreatedEvent(PublishedEvents events) {
        // When
        String userId = userService.createUser("testuser");
        
        // Then
        EventTestHelper.assertEventPublished(events, UserCreatedEvent.class);
        EventTestHelper.assertEventPublished(events, UserCreatedEvent.class, 
            event -> event.userId().equals(userId) && event.username().equals("testuser"));
    }
    
    @Test
    void shouldPublishUserCreatedEventWithAssertablePublishedEvents(AssertablePublishedEvents events) {
        // When
        String userId = userService.createUser("testuser2");
        
        // Then
        EventTestHelper.assertEventPublished(events, UserCreatedEvent.class);
        EventTestHelper.assertEventPublished(events, UserCreatedEvent.class, 
            event -> event.userId().equals(userId) && event.username().equals("testuser2"));
    }
    
    @Test
    void shouldCreateUser() {
        // When
        String userId = userService.createUser("testuser3");
        
        // Then
        assertThat(userId).isNotNull().isNotEmpty();
    }
}