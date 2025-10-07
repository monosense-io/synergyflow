package io.monosense.synergyflow.security.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Service for mapping Keycloak realm roles to application roles.
 *
 * <p>This service provides the public API for role mapping used by other modules for RBAC enforcement.
 * It translates Keycloak realm roles to standardized application roles used in @PreAuthorize annotations.
 *
 * <p>Role mapping (case-insensitive):
 * <ul>
 *   <li>Keycloak 'admin' → Application 'ADMIN' (full system access)</li>
 *   <li>Keycloak 'itsm_agent' → Application 'ITSM_AGENT' (ticket operations)</li>
 *   <li>Keycloak 'developer' → Application 'DEVELOPER' (PM operations)</li>
 *   <li>Keycloak 'manager' → Application 'MANAGER' (approval workflows)</li>
 *   <li>Keycloak 'employee' → Application 'EMPLOYEE' (self-service requests)</li>
 * </ul>
 *
 * <p>Unknown roles are logged and skipped (not mapped to any application role).
 *
 * <p>This class is part of the security module's public API (api package) and can be
 * injected by other modules for authorization purposes.
 */
@Service
public class RoleMapper {

    private static final Logger logger = LoggerFactory.getLogger(RoleMapper.class);

    /**
     * Maps Keycloak realm roles to application roles.
     *
     * <p>Performs case-insensitive matching and logs warnings for unknown roles.
     *
     * @param keycloakRoles list of Keycloak realm roles
     * @return set of mapped application roles (uppercase)
     */
    public Set<String> mapKeycloakRolesToAppRoles(List<String> keycloakRoles) {
        if (keycloakRoles == null || keycloakRoles.isEmpty()) {
            return new HashSet<>();
        }

        Set<String> appRoles = new HashSet<>();

        for (String keycloakRole : keycloakRoles) {
            if (keycloakRole == null || keycloakRole.isBlank()) {
                continue;
            }

            String normalizedRole = keycloakRole.toLowerCase(Locale.ROOT);

            switch (normalizedRole) {
                case "admin" -> appRoles.add("ADMIN");
                case "itsm_agent" -> appRoles.add("ITSM_AGENT");
                case "developer" -> appRoles.add("DEVELOPER");
                case "manager" -> appRoles.add("MANAGER");
                case "employee" -> appRoles.add("EMPLOYEE");
                default -> logger.warn("Unknown Keycloak role encountered: {}. Skipping role mapping.", keycloakRole);
            }
        }

        return appRoles;
    }
}
