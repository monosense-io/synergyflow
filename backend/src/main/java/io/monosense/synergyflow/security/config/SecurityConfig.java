package io.monosense.synergyflow.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration for OAuth2 Resource Server with JWT authentication.
 *
 * <p>This configuration:
 * <ul>
 *   <li>Enables JWT-based authentication via OAuth2 Resource Server pattern</li>
 *   <li>Validates JWT tokens using Keycloak's JWK endpoint (configured in application.yml)</li>
 *   <li>Secures /actuator/health endpoint requiring valid JWT Bearer token</li>
 *   <li>Permits /actuator/info endpoint publicly for health check monitoring</li>
 *   <li>Configures CORS for frontend access (localhost:5173 in development)</li>
 *   <li>Enforces stateless session management (no server-side sessions)</li>
 * </ul>
 *
 * <p>RBAC enforcement is delegated to controller methods using @PreAuthorize annotations.
 * Role mapping from Keycloak roles to application roles is handled by {@link io.monosense.synergyflow.security.api.RoleMapper}.
 *
 * @see io.monosense.synergyflow.security.api.JwtValidator
 * @see io.monosense.synergyflow.security.api.RoleMapper
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security with JWT authentication and authorization rules.
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Configure OAuth2 Resource Server with JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> {})
            )

            // Configure authorization rules
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/info").permitAll()  // Public health check
                .requestMatchers("/actuator/health").authenticated()  // Secured health endpoint
                .anyRequest().authenticated()  // All other endpoints require authentication
            )

            // Enable CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Disable CSRF for stateless API
            .csrf(csrf -> csrf.disable())

            // Stateless session management (no server-side sessions)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }

    /**
     * Configures CORS to allow frontend access from localhost:5173 (Vite dev server).
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
