package io.monosense.synergyflow.user.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts JWT claims to Spring Security authorities, supporting multiple IdP formats.
 * Supports Keycloak (realm_access.roles), Auth0/Okta (permissions, roles).
 *
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@Slf4j
@Component
public class JwtAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<String> roles = new HashSet<>();

        // Keycloak: realm_access.roles[]
        extractKeycloakRoles(jwt, roles);

        // Auth0/Okta: permissions[] and roles[]
        extractAuth0OktaRoles(jwt, roles);

        // Convert to Spring Security authorities
        return roles.stream()
                .filter(Objects::nonNull)
                .filter(role -> !role.trim().isEmpty())
                .map(this::normalizeRole)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private void extractKeycloakRoles(Jwt jwt, Set<String> roles) {
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                List<String> realmRoles = (List<String>) realmAccess.get("roles");
                if (realmRoles != null) {
                    roles.addAll(realmRoles);
                    log.debug("Extracted Keycloak realm roles: {}", realmRoles);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract Keycloak roles from JWT: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void extractAuth0OktaRoles(Jwt jwt, Set<String> roles) {
        try {
            // Auth0/Okta permissions
            List<String> permissions = jwt.getClaim("permissions");
            if (permissions != null) {
                roles.addAll(permissions);
                log.debug("Extracted Auth0/Okta permissions: {}", permissions);
            }

            // Auth0/Okta roles
            List<String> jwtRoles = jwt.getClaim("roles");
            if (jwtRoles != null) {
                roles.addAll(jwtRoles);
                log.debug("Extracted Auth0/Okta roles: {}", jwtRoles);
            }
        } catch (Exception e) {
            log.warn("Failed to extract Auth0/Okta roles from JWT: {}", e.getMessage());
        }
    }

    private String normalizeRole(String role) {
        return role.replace(':', '_')
                  .replace('-', '_')
                  .replace('.', '_')
                  .toUpperCase(Locale.ROOT);
    }
}