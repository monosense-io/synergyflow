package io.monosense.synergyflow.security.api;

import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for validating JWT tokens and extracting claims for authentication and authorization.
 *
 * <p>This service provides the public API for JWT validation used by other modules.
 * It decodes and validates JWT tokens issued by Keycloak, extracting essential claims
 * for authentication and authorization. The service leverages Spring Security's JWT
 * infrastructure to provide robust token validation with automatic key rotation support.</p>
 *
 * <p>Token validation includes:
 * <ul>
 *   <li>Signature verification using Keycloak's JWK public key</li>
 *   <li>Expiration time validation</li>
 *   <li>Issuer validation</li>
 *   <li>Claim extraction (subject, email, roles)</li>
 * </ul>
 *
 * <p>This class is part of the security module's public API (api package) and can be
 * injected by other modules for authentication purposes. It's active only in non-worker
 * profiles to avoid unnecessary validation in background processing contexts.</p>
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
 *         List<Ticket> tickets = ticketService.findByAssignee(claims.subject());
 *         return ResponseEntity.ok(tickets);
 *     }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 *
 * @see JwtClaims
 * @see InvalidTokenException
 */
@Service
@Profile("!worker")
public class JwtValidator {

    private final JwtDecoder jwtDecoder;

    /**
     * Constructs a new JwtValidator with the required JWT decoder.
     *
     * @param jwtDecoder the Spring Security JWT decoder configured for Keycloak validation
     * @throws IllegalArgumentException if jwtDecoder is null
     */
    public JwtValidator(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /**
     * Validates a JWT token and extracts claims.
     *
     * @param token the JWT token string (without "Bearer " prefix)
     * @return the extracted JWT claims
     * @throws InvalidTokenException if the token is invalid, expired, or malformed
     */
    public JwtClaims validateAndExtractClaims(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            String subject = jwt.getSubject();
            String email = jwt.getClaimAsString("email");
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            List<String> roles = extractRoles(jwt);

            return new JwtClaims(subject, email, preferredUsername, roles);

        } catch (JwtException e) {
            throw new InvalidTokenException("Failed to validate JWT token", e);
        } catch (Exception e) {
            throw new InvalidTokenException("Unexpected error during token validation", e);
        }
    }

    /**
     * Extracts Keycloak realm roles from the JWT token.
     *
     * <p>Keycloak stores realm roles in the 'realm_access.roles' claim structure:
     * <pre>
     * {
     *   "realm_access": {
     *     "roles": ["admin", "itsm_agent"]
     *   }
     * }
     * </pre>
     *
     * @param jwt the decoded JWT token
     * @return list of realm roles, or empty list if no roles found
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");

        if (realmAccess == null) {
            return new ArrayList<>();
        }

        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List<?>) {
            List<?> rolesList = (List<?>) rolesObj;
            return rolesList.stream()
                .filter(role -> role instanceof String)
                .map(role -> (String) role)
                .toList();
        }

        return new ArrayList<>();
    }
}
