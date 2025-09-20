package io.monosense.synergyflow.user.application;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.mockito.Mockito;

import org.springframework.boot.test.mock.mockito.MockBean;

import io.monosense.synergyflow.user.infrastructure.JwtAuthoritiesConverter;

/**
 * Integration tests for WhoAmIController using Spring Modulith patterns.
 * Tests JWT authentication flows with mock JWT tokens.
 *
 * @author monosense
 * @since 2025-09
 * @version 1.0
 */
@WebMvcTest({WhoAmIController.class, ActuatorStubController.class})
@Import({io.monosense.synergyflow.user.infrastructure.SecurityConfig.class, io.monosense.synergyflow.user.infrastructure.JwtAuthoritiesConverter.class})
@TestPropertySource(properties = "NEXTAUTH_URL=http://localhost:3000")
@DisplayName("WhoAmIController Integration")
class WhoAmIControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthoritiesConverter jwtAuthoritiesConverter;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    @DisplayName("should return 401 when no JWT token provided")
    void shouldReturn401WhenNoJwtToken() throws Exception {
        mockMvc.perform(get("/api/whoami"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return user info with valid JWT token")
    void shouldReturnUserInfoWithValidJwtToken() throws Exception {
        mockMvc.perform(get("/api/whoami")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("test-user")
                                        .claim("preferred_username", "testuser")
                                        .claim("email", "test@example.com")
                                        .claim("name", "Test User")
                                        .audience(List.of("synergyflow-api"))
                                        .issuer("https://test-issuer.example.com")
                                )
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("test-user"))
                .andExpect(jsonPath("$.preferredUsername").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_USER"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("should return 401 for admin endpoint without JWT")
    void shouldReturn401ForAdminEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/admin/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 for admin endpoint without ADMIN role")
    void shouldReturn403ForAdminEndpointWithoutAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("test-user")
                                        .audience(List.of("synergyflow-api"))
                                        .issuer("https://test-issuer.example.com")
                                )
                                .authorities(new SimpleGrantedAuthority("ROLE_USER"))
                        ))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should return 200 for admin endpoint with ADMIN role")
    void shouldReturn200ForAdminEndpointWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                        .with(jwt()
                                .jwt(jwt -> jwt
                                        .subject("admin-user")
                                        .audience(List.of("synergyflow-api"))
                                        .issuer("https://test-issuer.example.com")
                                )
                                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Admin ping successful"))
                .andExpect(jsonPath("$.adminUser").value("admin-user"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("should extract roles from Keycloak JWT format")
    void shouldExtractRolesFromKeycloakJwtFormat() throws Exception {
        mockMvc.perform(get("/api/whoami")
                        .with(authenticatedJwt(jwt -> jwt
                                .subject("keycloak-user")
                                .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("keycloak-user"))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities").value(org.hamcrest.Matchers.containsInAnyOrder("ROLE_USER", "ROLE_ADMIN")));
    }

    @Test
    @DisplayName("should extract roles from Auth0/Okta JWT format")
    void shouldExtractRolesFromAuth0OktaJwtFormat() throws Exception {
        mockMvc.perform(get("/api/whoami")
                        .with(authenticatedJwt(jwt -> jwt
                                .subject("auth0-user")
                                .claim("permissions", List.of("read:data", "write:data"))
                                .claim("roles", List.of("user"))
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("auth0-user"))
                .andExpect(jsonPath("$.authorities").isArray())
                .andExpect(jsonPath("$.authorities").value(
                        org.hamcrest.Matchers.containsInAnyOrder("ROLE_READ_DATA", "ROLE_WRITE_DATA", "ROLE_USER")
                ));
    }

    @Test
    @DisplayName("should allow access to actuator health endpoint without authentication")
    void shouldAllowAccessToActuatorHealthWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should allow access to actuator info endpoint without authentication")
    void shouldAllowAccessToActuatorInfoWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when JWT has invalid audience")
    void shouldReturn401WhenJwtHasInvalidAudience() throws Exception {
        String token = "invalid-audience-token";
        Mockito.when(jwtDecoder.decode(token))
                .thenThrow(new JwtValidationException("Required audience missing",
                        List.of(new OAuth2Error("invalid_token", "Required audience missing", null))));

        mockMvc.perform(get("/api/whoami")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 401 when JWT is expired")
    void shouldReturn401WhenJwtIsExpired() throws Exception {
        String token = "expired-token";
        Mockito.when(jwtDecoder.decode(token))
                .thenThrow(new JwtValidationException("JWT expired",
                        List.of(new OAuth2Error("invalid_token", "JWT expired", null))));

        mockMvc.perform(get("/api/whoami")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should expose correlation id header")
    void shouldExposeCorrelationIdHeader() throws Exception {
        mockMvc.perform(get("/api/whoami")
                        .with(authenticatedJwt(jwt -> jwt
                                .subject("correlated-user")
                                .claim("realm_access", Map.of("roles", List.of("user")))
                        ))
                        .header("X-Correlation-ID", "trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-ID", "trace-123"));
    }

    @Test
    @DisplayName("should allow configured CORS origin")
    void shouldAllowConfiguredCorsOrigin() throws Exception {
        mockMvc.perform(options("/api/whoami")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"));
    }

    private RequestPostProcessor authenticatedJwt(Consumer<Jwt.Builder> jwtBuilderCustomizer) {
        Instant now = Instant.now();
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("test-user")
                .issuer("https://test-issuer.example.com")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .audience(List.of("synergyflow-api"));

        jwtBuilderCustomizer.accept(builder);

        JwtAuthenticationConverter converter = jwtAuthenticationConverter();
        AbstractAuthenticationToken authentication = Objects.requireNonNull(converter.convert(builder.build()));
        return SecurityMockMvcRequestPostProcessors.authentication(authentication);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtAuthoritiesConverter);
        return converter;
    }
}
