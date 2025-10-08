package io.monosense.synergyflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the SynergyFlow application.
 *
 * <p>SynergyFlow is an integrated IT Service Management (ITSM) and Project Management (PM) platform
 * built with Spring Boot and Spring Modulith. This application provides event-driven architecture
 * with domain modules for ITSM ticketing, project issue tracking, and real-time event processing.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>Modular architecture with Spring Modulith</li>
 *   <li>Event-driven communication between domains</li>
 *   <li>JWT-based authentication with Keycloak</li>
 *   <li>Transactional outbox pattern for reliable event delivery</li>
 *   <li>Real-time updates with Server-Sent Events (SSE)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * // Run the application
 * java -jar synergyflow.jar
 *
 * // Or run with specific profile
 * java -jar synergyflow.jar --spring.profiles.active=dev
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
@SpringBootApplication
public class SynergyFlowApplication {

    /**
     * Main method that starts the Spring Boot application.
     *
     * @param args command line arguments (not used, but required for standard Java application entry point)
     */
    public static void main(String[] args) {
        SpringApplication.run(SynergyFlowApplication.class, args);
    }

}
