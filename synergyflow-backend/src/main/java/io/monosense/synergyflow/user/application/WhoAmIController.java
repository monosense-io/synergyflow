package io.monosense.synergyflow.user.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for user authentication and authorization debugging endpoints.
 * Provides /api/whoami for debugging user context and /api/admin/ping for admin role testing.
 *
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WhoAmIController {

    /**
     * Returns the current user's JWT subject and roles for debugging purposes.
     * Requires a valid JWT token.
     *
     * @param authentication Spring Security authentication context
     * @return user information including subject and authorities
     */
    @GetMapping("/whoami")
    public ResponseEntity<Map<String, Object>> whoAmI(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            response.put("subject", jwt.getSubject());
            response.put("issuer", jwt.getIssuer());
            response.put("audience", jwt.getAudience());
            response.put("issuedAt", jwt.getIssuedAt());
            response.put("expiresAt", jwt.getExpiresAt());

            List<String> authorities = authentication.getAuthorities()
                    .stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();
            response.put("authorities", authorities);

            // Include some safe claims for debugging
            response.put("preferredUsername", jwt.getClaimAsString("preferred_username"));
            response.put("email", jwt.getClaimAsString("email"));
            response.put("name", jwt.getClaimAsString("name"));

            log.debug("WhoAmI request for subject: {}, authorities: {}", jwt.getSubject(), authorities);
        } else {
            response.put("error", "No valid JWT authentication found");
        }

        response.put("timestamp", Instant.now());
        return ResponseEntity.ok(response);
    }

    /**
     * Admin-only endpoint for testing role-based access control.
     * Requires ROLE_ADMIN authority.
     *
     * @param authentication Spring Security authentication context
     * @return simple ping response for admin users
     */
    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminPing(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin ping successful");
        response.put("timestamp", Instant.now());

        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            response.put("adminUser", jwt.getSubject());
            log.info("Admin ping request from subject: {}", jwt.getSubject());
        }

        return ResponseEntity.ok(response);
    }
}