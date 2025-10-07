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
