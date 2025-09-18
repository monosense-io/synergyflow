package io.monosense.synergyflow.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing users.
 * 
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Creates a new user and publishes a UserCreatedEvent.
     * 
     * @param username the username
     * @return the user ID
     */
    public String createUser(String username) {
        String userId = UUID.randomUUID().toString();
        
        // Publish event
        eventPublisher.publishEvent(new UserCreatedEvent(userId, username, Instant.now()));
        
        return userId;
    }
}