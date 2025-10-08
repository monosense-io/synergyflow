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
 * It translates Keycloak realm roles to standardized application roles used in {@code @PreAuthorize} annotations.
 *
 * <p><strong>Role Mapping (case-insensitive):</strong>
 * <ul>
 *   <li>Keycloak 'admin' → Application 'ADMIN' (full system access)</li>
 *   <li>Keycloak 'itsm_agent' → Application 'ITSM_AGENT' (ticket operations)</li>
 *   <li>Keycloak 'developer' → Application 'DEVELOPER' (PM operations)</li>
 *   <li>Keycloak 'manager' → Application 'MANAGER' (approval workflows)</li>
 *   <li>Keycloak 'employee' → Application 'EMPLOYEE' (self-service requests)</li>
 * </ul>
 *
 * <p><strong>Role Permissions:</strong>
 * <ul>
 *   <li><strong>ADMIN:</strong> Full system access, user management, system configuration</li>
 *   <li><strong>ITSM_AGENT:</strong> Create, update, assign tickets; manage IT service operations</li>
 *   <li><strong>DEVELOPER:</strong> Create and manage issues, update project status</li>
 *   <li><strong>MANAGER:</strong> Approve requests, review reports, team oversight</li>
 *   <li><strong>EMPLOYEE:</strong> Create tickets, view own tickets, basic self-service</li>
 * </ul>
 *
 * <p><strong>Error Handling:</strong>
 * Unknown roles are logged at WARN level and skipped (not mapped to any application role).
 * This prevents system failures when Keycloak contains roles not recognized by the application.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class TicketService {
 *     @Autowired
 *     private RoleMapper roleMapper;
 *
 *     public boolean hasTicketAccess(List<String> keycloakRoles) {
 *         Set<String> appRoles = roleMapper.mapKeycloakRolesToAppRoles(keycloakRoles);
 *         return appRoles.contains("ITSM_AGENT") || appRoles.contains("ADMIN");
 *     }
 * }
 * }</pre>
 *
 * <p>This class is part of the security module's public API (api package) and can be
 * injected by other modules for authorization purposes. It follows the Spring Service pattern
 * and is automatically discovered via component scanning.
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see io.monosense.synergyflow.security.api.JwtValidator
 * @see io.monosense.synergyflow.security.api.JwtClaims
 */
@Service
public class RoleMapper {

    private static final Logger logger = LoggerFactory.getLogger(RoleMapper.class);

    /**
     * Maps Keycloak realm roles to application roles.
     *
     * <p>This method performs the core role translation logic, converting Keycloak realm roles
     * to standardized application roles used throughout the SynergyFlow system. The mapping
     * is case-insensitive to ensure robustness against variations in role naming.
     *
     * <p><strong>Mapping Rules:</strong>
     * <ul>
     *   <li>null or empty input → empty set</li>
     *   <li>null or blank individual roles → skipped</li>
     *   <li>unknown roles → logged as WARN and skipped</li>
     *   <li>valid roles → mapped to uppercase application roles</li>
     * </ul>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * // Input: ["admin", "itsm_agent", "unknown_role"]
     * // Output: Set.of("ADMIN", "ITSM_AGENT")
     * // Log: WARN - Unknown Keycloak role encountered: unknown_role. Skipping role mapping.
     * }</pre>
     *
     * @param keycloakRoles list of Keycloak realm roles from JWT token claims.
     *                     Can be null or empty, in which case an empty set is returned.
     * @return set of mapped application roles in uppercase format.
     *         Never null, but may be empty if no valid roles are found.
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
