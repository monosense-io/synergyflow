package io.monosense.synergyflow.security.api;

import java.util.List;

/**
 * Represents claims extracted from a validated JWT token.
 *
 * <p>This record encapsulates the essential claims needed for authentication and authorization:
 * <ul>
 *   <li>subject: The user's unique identifier (JWT 'sub' claim)</li>
 *   <li>email: The user's email address (JWT 'email' claim)</li>
 *   <li>preferredUsername: The user's preferred username (JWT 'preferred_username' claim)</li>
 *   <li>roles: List of Keycloak realm roles (extracted from 'realm_access.roles' claim)</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @RestController
 * class TicketController {
 *     private final JwtValidator jwtValidator;
 *
 *     public ResponseEntity<List<Ticket>> getTickets(
 *             @RequestHeader("Authorization") String authHeader) {
 *         String token = authHeader.substring(7); // Remove "Bearer " prefix
 *         JwtClaims claims = jwtValidator.validateAndExtractClaims(token);
 *
 *         // Use claims for authorization or user identification
 *         log.info("Access requested by user: {}", claims.preferredUsername());
 *         log.info("User roles: {}", claims.roles());
 *
 *         // Example: Check if user has required role
 *         if (claims.roles().contains("itsm_agent")) {
 *             List<Ticket> tickets = ticketService.findAssignedTickets(claims.subject());
 *             return ResponseEntity.ok(tickets);
 *         } else {
 *             return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
 *         }
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 *
 * @param subject the unique user identifier (sub claim)
 * @param email the user's email address
 * @param preferredUsername the user's preferred username
 * @param roles list of Keycloak realm roles
 */
public record JwtClaims(
    String subject,
    String email,
    String preferredUsername,
    List<String> roles
) {
}
